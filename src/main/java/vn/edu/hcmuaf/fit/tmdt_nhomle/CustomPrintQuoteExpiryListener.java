package vn.edu.hcmuaf.fit.tmdt_nhomle;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CustomPrintWorkflowDao;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebListener
public final class CustomPrintQuoteExpiryListener implements ServletContextListener {
    private static final System.Logger LOG = System.getLogger(CustomPrintQuoteExpiryListener.class.getName());
    private ScheduledExecutorService executor;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "printkraft-custom-quote-expiry");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(() -> {
            try { CustomPrintWorkflowDao.expireQuotes(); }
            catch (RuntimeException e) { LOG.log(System.Logger.Level.WARNING, "Custom quote expiry run failed", e); }
        }, 1, 5, TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (executor != null) executor.shutdownNow();
    }
}
