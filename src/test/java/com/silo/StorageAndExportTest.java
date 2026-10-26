package com.silo;

import com.silo.service.*;
import com.silo.storage.FileStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StorageAndExportTest {
    @TempDir Path dir;

    @Test void csvQuotesCommasQuotesAndNewlinesAndStartsWithBom() throws Exception {
        Path f = dir.resolve("x.csv");
        CsvExporter.write(f, new String[]{"A", "B"}, List.of(new String[]{"plain", "has,comma"}, new String[]{"say \"hi\"", "two\nlines"}));
        String text = Files.readString(f, StandardCharsets.UTF_8);
        assertTrue(text.startsWith("\uFEFFA,B\r\n"));
        assertTrue(text.contains("plain,\"has,comma\"\r\n"));
        assertTrue(text.contains("\"say \"\"hi\"\"\",\"two\nlines\"\r\n"));
    }

    @Test void csvWriteToMissingFolderGivesReadableError() {
        assertThrows(ValidationException.class, () -> CsvExporter.write(dir.resolve("nope/none.csv"), new String[]{"A"}, List.of()));
    }

    @Test void sampleDataLoadsThroughNormalRulesAndReportsMatch() {
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 10, 26, 8, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        SiloService s = new SiloService(new FileStore(dir), clock);
        SampleData.load(s);
        assertEquals(5, s.donations().size());
        assertEquals(3, s.volunteers().size());
        assertEquals(3, s.recipients().size());
        assertEquals(1, s.queueOrder().size());
        assertEquals(665.0, s.donatedByUnit().get("kg"), 1e-9);
        assertEquals(5, s.donationRows().size());
    }

    @Test void distributionReportFiltersByDateFoodAndDonor() {
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 10, 26, 8, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        SiloService s = new SiloService(new FileStore(dir), clock);
        LocalDate t = s.today();
        var corn = s.addDonation("Farm A", "Corn", "40", "kg", "QC", "3", 3, t, t.plusDays(5));
        var rice = s.addDonation("Farm B", "Rice", "40", "kg", "QC", "3", 1, t, t.plusDays(50));
        var v = s.addVolunteer("Ben", "09171234567", "QC", "Any");
        var r = s.addRecipient("Shelter", "", "Addr", "Shelter");
        s.markPickedUp(s.schedulePickup(corn.getId(), v.getId(), LocalDateTime.of(2026, 10, 27, 9, 0)).getId());
        s.markPickedUp(s.schedulePickup(rice.getId(), v.getId(), LocalDateTime.of(2026, 10, 27, 11, 0)).getId());
        s.recordDistribution(corn.getId(), r.getId(), "10", t.minusDays(3));
        s.recordDistribution(rice.getId(), r.getId(), "15.5", t);
        assertEquals(2, s.distributionRows(null, null, "", "").size());
        assertEquals(1, s.distributionRows(t.minusDays(1), null, "", "").size());
        assertEquals("Rice", s.distributionRows(null, null, "ric", "").get(0)[2]);
        assertEquals(1, s.distributionRows(null, null, "", "farm a").size());
        assertEquals("15.5", s.distributionRows(null, null, "rice", "").get(0)[5]);
        assertEquals(0, s.distributionRows(t.plusDays(1), null, "", "").size());
    }

    @Test void corruptingNothingLeavesNoTempFilesBehind() {
        SiloService s = new SiloService(new FileStore(dir));
        s.addVolunteer("Ana", "09171234567", "QC", "Any");
        assertFalse(Files.exists(dir.resolve("volunteers.tsv.tmp")));
        assertTrue(Files.exists(dir.resolve("volunteers.tsv")));
    }
}
