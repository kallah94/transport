package com.gayale.transport.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void clean() {
        TenantContext.clear();
    }

    @Test
    void set_get_clear() {
        assertThat(TenantContext.getTenantId()).isNull();

        TenantContext.setTenantId("tenant-1");
        assertThat(TenantContext.getTenantId()).isEqualTo("tenant-1");

        TenantContext.clear();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void isolated_per_thread() throws InterruptedException {
        TenantContext.setTenantId("main-tenant");

        final String[] seenInOtherThread = new String[1];
        Thread t = new Thread(() -> seenInOtherThread[0] = TenantContext.getTenantId());
        t.start();
        t.join();

        // Le ThreadLocal ne fuit pas d'un thread a l'autre.
        assertThat(seenInOtherThread[0]).isNull();
        assertThat(TenantContext.getTenantId()).isEqualTo("main-tenant");
    }
}
