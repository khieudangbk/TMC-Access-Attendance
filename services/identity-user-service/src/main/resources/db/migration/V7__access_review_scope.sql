UPDATE oauth2_registered_client SET scopes = scopes || ',access.review' WHERE client_id = 'tmc-web' AND scopes NOT LIKE '%access.review%';
