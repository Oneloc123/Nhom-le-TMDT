package vn.edu.hcmuaf.fit.tmdt_nhomle;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.DeliveryAutomationDao;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Container-managed scheduler; tasks are bounded and safe to repeat. */
@WebListener
public final class DeliveryAutomationListener implements ServletContextListener {
    private static final System.Logger LOG = System.getLogger(DeliveryAutomationListener.class.getName());
    private ScheduledExecutorService executor;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        executor = Executors.newScheduledThreadPool(2, task -> {
            Thread thread = new Thread(task, "printkraft-delivery-automation");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(() -> {
            try { DeliveryAutomationDao.processReceiptAutomation(); }
            catch (RuntimeException error) {
                LOG.log(System.Logger.Level.WARNING, "Receipt reminder/auto-confirmation run failed.", error);
            }
        }, 1, 5, TimeUnit.MINUTES);
        executor.scheduleWithFixedDelay(() -> {
            try { DeliveryAutomationDao.pollGhnShipments(); }
            catch (RuntimeException error) {
                LOG.log(System.Logger.Level.WARNING, "GHN fallback polling run failed.", error);
            }
        }, 2, 15, TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (executor != null) executor.shutdownNow();
    }
}
