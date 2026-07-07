import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import test from 'node:test'
import assert from 'node:assert/strict'

const nginxConf = readFileSync(resolve('nginx.conf'), 'utf8')
const envExample = readFileSync(resolve('../deploy/.env.example'), 'utf8')

test('frontend proxies OnlyOffice through the public entrypoint', () => {
  assert.match(nginxConf, /location\s+\^~\s+\/onlyoffice\//)
  assert.match(nginxConf, /proxy_pass\s+http:\/\/onlyoffice\//)
  assert.match(nginxConf, /proxy_set_header\s+Upgrade\s+\$http_upgrade/)
  assert.match(nginxConf, /proxy_redirect\s+~\^https\?:\/\/\[\^\/\]\+\/\(\.\*\)\$\s+\/onlyoffice\/\$1/)
  assert.match(nginxConf, /proxy_redirect\s+\/\s+\/onlyoffice\//)
  assert.match(envExample, /^ONLYOFFICE_URL=\/onlyoffice$/m)
  assert.doesNotMatch(envExample, /^ONLYOFFICE_URL=http:\/\/localhost:8082$/m)
})
