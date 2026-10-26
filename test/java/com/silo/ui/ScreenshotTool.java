package com.silo.ui;

import com.silo.service.*;
import com.silo.storage.FileStore;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;

/** Developer tool (not a unit test): renders each page to a PNG so the layout can be checked, including empty state. */
public class ScreenshotTool {
    public static void main(String[] args) throws Exception {
        Path data = Files.createTempDirectory("silo-shot");
        Path out = Path.of(args[0]); Files.createDirectories(out);
        SiloService svc = new SiloService(new FileStore(data));
        SwingUtilities.invokeAndWait(() -> {
            Ui.installTheme();
            SiloApp app = new SiloApp(svc);
            app.setVisible(true);
            try {
                shot(app, out, "00-dashboard-empty");
                app.act("Sample data loaded", () -> SampleData.load(svc));
                for (String p : new String[]{"Dashboard", "Donations", "Pickups", "Volunteers", "Recipients", "Reports"}) {
                    app.show(p); shot(app, out, "10-" + p.toLowerCase());
                }
                // simulate a full workflow through the app's own actions
                app.act("ok", () -> svc.processNextPickup());
                app.act("ok", () -> svc.recordDistribution(1, 1, "50", svc.today()));
                app.act(null, () -> svc.schedulePickup(2, 2, svc.now().minusDays(1)));  // invalid -> error toast
                app.show("Recipients"); shot(app, out, "20-recipients-after");
                app.show("Pickups"); shot(app, out, "21-pickups-error");
                app.show("Reports"); shot(app, out, "22-reports");
                // dialog render
                DonationsPage.DonationDialog dlg = new DonationsPage.DonationDialog(app, svc, null);
                dlg.setModalityType(Dialog.ModalityType.MODELESS);
                dlg.pack(); dlg.setLocation(300, 100); dlg.setVisible(true);
                try { dlg.getClass().getSuperclass().getDeclaredMethod("setError", String.class).setAccessible(true); } catch (Exception ignored) { }
                BufferedImage img = new BufferedImage(dlg.getWidth(), dlg.getHeight(), BufferedImage.TYPE_INT_RGB);
                dlg.paintAll(img.getGraphics());
                javax.imageio.ImageIO.write(img, "png", out.resolve("30-dialog.png").toFile());
            } catch (Exception e) { e.printStackTrace(); }
            System.out.println("done");
            System.exit(0);
        });
    }
    static void shot(SiloApp app, Path out, String name) throws Exception {
        app.validate();
        BufferedImage img = new BufferedImage(app.getWidth(), app.getHeight(), BufferedImage.TYPE_INT_RGB);
        app.paintAll(img.getGraphics());
        javax.imageio.ImageIO.write(img, "png", out.resolve(name + ".png").toFile());
    }
}
