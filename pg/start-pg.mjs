import EmbeddedPostgres from 'embedded-postgres';
import fs from 'node:fs';

const dataDir = '/workspace/pg/data';
if (process.argv[2] === '--clean' && fs.existsSync(dataDir)) {
  fs.rmSync(dataDir, { recursive: true, force: true });
}

const pg = new EmbeddedPostgres({
  databaseDir: dataDir,
  user: 'licensing',
  password: 'licensing',
  port: 5432,
  persistent: true,
  postgresFlags: ['-c', 'timezone=UTC', '-c', 'log_timezone=UTC'],
});

const fresh = !fs.existsSync(`${dataDir}/PG_VERSION`);
if (fresh) {
  await pg.initialise();
}
await pg.start();
if (fresh) {
  await pg.createDatabase('licensing');
  console.log('database "licensing" created');
}
console.log('PostgreSQL READY on port 5432 (db=licensing, user=licensing, tz=UTC)');

function shutdown() {
  pg.stop().then(() => process.exit(0)).catch(() => process.exit(1));
}
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
