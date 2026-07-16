import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import test from 'node:test'
import assert from 'node:assert/strict'

const runScript = readFileSync(resolve('../run.sh'), 'utf8')

test('container database maintenance uses the same TCP connection as health checks', () => {
  assert.match(runScript, /mysql -h127\.0\.0\.1 -u"\$MYSQL_USER"/)
  assert.match(runScript, /mysqldump -h127\.0\.0\.1 -u"\$MYSQL_USER"/)
})
