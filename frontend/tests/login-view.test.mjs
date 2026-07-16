import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import test from 'node:test'
import assert from 'node:assert/strict'

const loginView = readFileSync(resolve('src/views/LoginView.vue'), 'utf8')
const formDeclaration = loginView.match(/const form = reactive\((\{[^)]*\})\)/s)?.[1] ?? ''
const authStore = readFileSync(resolve('src/stores/auth.ts'), 'utf8')
const httpClient = readFileSync(resolve('src/api/http.ts'), 'utf8')
const detailView = readFileSync(resolve('src/views/DocItemDetailView.vue'), 'utf8')
const personalSpaceView = readFileSync(resolve('src/views/PersonalSpaceView.vue'), 'utf8')
const errorReporter = readFileSync(resolve('src/utils/errorReporter.ts'), 'utf8')
const layoutView = readFileSync(resolve('src/views/LayoutView.vue'), 'utf8')
const orgFilesView = readFileSync(resolve('src/views/OrgFilesView.vue'), 'utf8')
const dashboardView = readFileSync(resolve('src/views/DashboardView.vue'), 'utf8')
const deptView = readFileSync(resolve('src/views/DeptView.vue'), 'utf8')
const orgHomeRedirectView = readFileSync(resolve('src/views/OrgHomeRedirectView.vue'), 'utf8')

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

test('system error console lives in notifications and is super admin only', () => {
  assert.match(personalSpaceView, /系统错误/)
  assert.match(personalSpaceView, /isSuperAdmin/)
  assert.match(personalSpaceView, /ErrorEventPanel/)
})

test('frontend reports runtime errors without recursive reporting or secrets', () => {
  assert.match(errorReporter, /installErrorReporter/)
  assert.match(errorReporter, /window\.addEventListener\(['"]error['"]/)
  assert.match(errorReporter, /window\.addEventListener\(['"]unhandledrejection['"]/)
  assert.match(errorReporter, /reporting/)
  assert.doesNotMatch(errorReporter, /password/)
  assert.doesNotMatch(errorReporter, /Authorization/)
})

test('frontend skips error event submission before login csrf cookie exists', () => {
  assert.match(errorReporter, /const csrfToken = readCookie\(['"]XSRF-TOKEN['"]\)/)
  assert.match(errorReporter, /if\s*\(!csrfToken\)\s*{\s*return\s*}/)
})

test('module layout puts home in the top left and removes sidebar home', () => {
  assert.match(layoutView, /<div class="top-actions top-left">[\s\S]*router\.push\('\/dashboard'\)[\s\S]*首页[\s\S]*<\/div>/)
  assert.doesNotMatch(layoutView, />资料目录<\/el-button>/)
  assert.doesNotMatch(layoutView, /<el-menu-item\s+index="\/dashboard">首页<\/el-menu-item>/)
})

test('module layout lists sections directly in navigation order for both modules', () => {
  assert.doesNotMatch(layoutView, /<el-sub-menu\s+index="sections"/)
  assert.doesNotMatch(layoutView, /<template\s+#title>科室<\/template>/)
  assert.match(layoutView, /<el-menu-item\s+v-for="dept in navigation"[\s\S]*:index="`\/\$\{moduleBase\}\/\$\{dept\.id\}`"/)
  assert.match(layoutView, /route\.path\.startsWith\('\/rules'\)\s*\?\s*'rules'\s*:\s*'internal'/)
  assert.match(layoutView, /moduleType:\s*moduleBase\.value\s*={2,3}\s*'rules'\s*\?\s*'RULES'\s*:\s*'INTERNAL'/)
  assert.match(orgHomeRedirectView, /moduleType/)
})

test('module layout reloads navigation whenever a module route is entered', () => {
  assert.match(layoutView, /watch\(\(\)\s*=>\s*route\.path/)
  assert.match(layoutView, /if\s*\(isModuleRoute\.value\)\s*{\s*loadNavigation\(\)/)
})

test('dashboard keeps only the two blue module titles', () => {
  assert.match(dashboardView, />内业资料</)
  assert.match(dashboardView, />技术规章、文件、管理办法</)
  assert.doesNotMatch(dashboardView, /module-card-desc/)
  assert.doesNotMatch(dashboardView, /车间上传信息/)
  assert.doesNotMatch(dashboardView, /制度文件展示、预览和下载/)
})

test('org files view restores completion progress without workshop upload gates', () => {
  assert.doesNotMatch(orgFilesView, /type="success">上传<\/el-tag>/)
  assert.doesNotMatch(orgFilesView, /upload-tag/)
  assert.doesNotMatch(orgFilesView, /允许车间上传/)
  assert.doesNotMatch(orgFilesView, /可上传车间/)
  assert.match(orgFilesView, /使用完成进度条/)
  assert.match(orgFilesView, /完成目标数/)
  assert.match(orgFilesView, /directFileCount/)
  assert.match(orgFilesView, /completionPercent/)
  assert.match(orgFilesView, /isInternalModule/)
})

test('organization console renders fixed navigation departments as read only', () => {
  assert.match(deptView, /fixedNavigation/)
  assert.match(deptView, />固定侧边栏</)
  assert.match(deptView, /v-if="!row\.fixedNavigation"[\s\S]*@click="edit\(row\)"/)
})

test('internal workshop file creation is not gated by folder upload config', () => {
  const functionBody = orgFilesView.match(/function canCreateFileInFolder\(node: DocNode\) \{([\s\S]*?)\n\}/)?.[1] ?? ''
  assert.match(functionBody, /isInternalModule\.value/)
  assert.match(functionBody, /node\.nodeType\s*={2,3}\s*'FOLDER'/)
  assert.match(functionBody, /isWorkshopUser\.value/)
  assert.doesNotMatch(functionBody, /workshopUploadEnabled/)
  assert.doesNotMatch(functionBody, /visibleWorkshopIds/)
})
