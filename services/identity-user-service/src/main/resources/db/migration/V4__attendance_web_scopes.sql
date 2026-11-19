UPDATE oauth2_registered_client
SET scopes = scopes || ',attendance.write,hr.export'
WHERE client_id = 'tmc-web' AND scopes NOT LIKE '%attendance.write%';
