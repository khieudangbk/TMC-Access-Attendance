-- Runs only on first initialization of the Postgres volume.
-- The central API uses tmc_central; each edge factory has its own local database.
CREATE DATABASE tmc_edge OWNER tmc;
