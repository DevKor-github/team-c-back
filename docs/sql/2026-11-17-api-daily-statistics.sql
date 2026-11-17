CREATE TABLE api_daily_statistics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stat_date DATE NOT NULL,
    environment VARCHAR(20) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    call_count BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_api_daily_statistics_date_env_endpoint_method
        UNIQUE (stat_date, environment, endpoint, method)
);
