import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import test from 'node:test'
import assert from 'node:assert/strict'

const loginView = readFileSync(resolve('src/views/LoginView.vue'), 'utf8')
const formDeclaration = loginView.match(/const form = reactive\((\{[^)]*\})\)/s)?.[1] ?? ''
const authStore = readFileSync(resolve('src/stores/auth.ts'), 'utf8')
const httpClient = readFileSync(resolve('src/api/http.ts'), 'utf8')
const detailView = readFileSync(resolve('src/views/DocItemDetailView.vue'), 'utf8')

test('login form does not prefill the seeded demo account', () => {
  assert.match(formDeclaration, /password:\s*['"]['"]/)
  assert.doesNotMatch(formDeclaration, /00000000000/)
  assert.doesNotMatch(formDeclaration, /Admin12345@@/)
})

test('login form remembers only phone and keeps password out of storage', () => {
  assert.match(loginView, /REMEMBER_PHONE_KEY/)
  assert.match(loginView, /localStorage\.setItem\(REMEMBER_PHONE_KEY,\s*form\.phone\.trim\(\)\)/)
  assert.doesNotMatch(loginView, /localStorage\.setItem\([^)]*password/i)
  assert.match(loginView, /autocomplete="current-password"/)
})

test('frontend keeps login jwt out of script-visible storage and urls', () => {
  assert.doesNotMatch(authStore, /localStorage\.getItem\(['"]token['"]\)/)
  assert.doesNotMatch(authStore, /localStorage\.setItem\(['"]token['"]/)
  assert.doesNotMatch(authStore, /localStorage\.setItem\(['"]permissions['"]/)
  assert.doesNotMatch(httpClient, /Authorization\s*=\s*`Bearer/)
  assert.match(httpClient, /withCredentials:\s*true/)
  assert.match(httpClient, /X-XSRF-TOKEN/)
  assert.doesNotMatch(detailView, /access_token/)
})
