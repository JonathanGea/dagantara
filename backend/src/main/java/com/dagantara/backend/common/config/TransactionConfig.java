package com.dagantara.backend.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Feature services choose their own explicit @Transactional boundaries. */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {
}
