UPDATE oauth2_registered_client SET scopes = scopes || ',audit.read' WHERE client_id = 'tmc-web' AND scopes NOT LIKE '%audit.read%';
