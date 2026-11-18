const auditDatabase = db.getSiblingDB('audit_notification_db');
if (!process.env.TMC_MONGO_PASSWORD) {
  throw new Error('TMC_MONGO_PASSWORD is required');
}
// Provision collections/indexes and tmcAuditApplication via audit-role.js first.
const applicationRoles = [{ role: 'tmcAuditApplication', db: 'audit_notification_db' }];
if (auditDatabase.getUser('audit_app')) {
  auditDatabase.updateUser('audit_app', {pwd: process.env.TMC_MONGO_PASSWORD, roles: applicationRoles});
} else {
  auditDatabase.createUser({user: 'audit_app', pwd: process.env.TMC_MONGO_PASSWORD, roles: applicationRoles});
}
