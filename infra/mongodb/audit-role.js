// Run once as database administrator. Provision the application user separately with this role.
const roleDatabase = db.getSiblingDB("audit_notification_db");
for (const collectionName of ["audit_events", "notification_jobs"]) {
    if (!roleDatabase.getCollectionNames().includes(collectionName)) {
        roleDatabase.createCollection(collectionName);
    }
}
roleDatabase.audit_events.createIndex({factoryId: 1, occurredAt: -1, _id: 1});
roleDatabase.notification_jobs.createIndex({status: 1, nextAttemptAt: 1, leaseUntil: 1});
const applicationRole = {
    role: "tmcAuditApplication",
    privileges: [
        {resource: {db: "audit_notification_db", collection: "audit_events"}, actions: ["find", "insert"]},
        {resource: {db: "audit_notification_db", collection: "notification_jobs"}, actions: ["find", "insert", "update"]}
    ],
    roles: []
};
if (roleDatabase.getRole(applicationRole.role)) {
    roleDatabase.updateRole(applicationRole.role, {privileges: applicationRole.privileges, roles: []});
} else {
    roleDatabase.createRole(applicationRole);
}
