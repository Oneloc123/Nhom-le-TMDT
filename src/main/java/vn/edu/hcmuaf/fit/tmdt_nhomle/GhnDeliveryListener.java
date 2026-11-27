package vn.edu.hcmuaf.fit.tmdt_nhomle;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.GhnDeliveryDao;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebListener
public final class GhnDeliveryListener implements ServletContextListener {
    private static final System.Logger LOG = System.getLogger(GhnDeliveryListener.class.getName());
    private ScheduledExecutorService executor;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        executor = Executors.newScheduledThreadPool(2, task -> {
            Thread thread = new Thread(task, "printkraft-ghn-delivery");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(() -> run(GhnDeliveryDao::syncActive), 10, 30, TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay(() -> run(GhnDeliveryDao::completeDue), 15, 60, TimeUnit.SECONDS);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (executor != null) executor.shutdownNow();
    }

    private static void run(Runnable operation) {
        try { operation.run(); }
        catch (RuntimeException e) { LOG.log(System.Logger.Level.WARNING, "GHN delivery job failed", e); }
    }
}
