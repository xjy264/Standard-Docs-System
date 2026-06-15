# File Dialog Year Radio Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 文件新增/编辑弹窗中用单选框表达车间上传选项，并隐藏文件年份选择，让文件年份继承所在年度目录。

**Architecture:** 保持现有 `sys_doc_item.doc_year` 和接口字段不变，只调整前端表单来源与展示；后端补充一条父目录年份继承测试，保护已有服务行为。文档同步说明年份由目录继承，避免后续重新加回手填年份。

**Tech Stack:** Vue 3、Element Plus、TypeScript、Spring Boot 3、JUnit 5、Mockito。

---

### Task 1: 后端年份继承回归测试

**Files:**
- Modify: `backend/src/test/java/cn/datong/standard/service/DocWorkspaceServiceTest.java`

- [ ] **Step 1: Write the failing test**

Add a test showing that creating a file without an explicit `docYear` inherits the parent folder year:

```java
@Test
void createFileWithMainFileInheritsParentFolderYearWhenDocYearMissing() {
    Fixtures fx = fixtures();
    when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
    SysDocNode parent = node(5L, 2L, null, "FOLDER", "2028年度资料", null, 1, 10);
    parent.setDocYear(2028);
    when(fx.nodeMapper.selectById(5L)).thenReturn(parent);
    when(fx.storageService.upload(any(), any())).thenReturn(new StoredObject("standard-docs", "doc-items/test.pdf", 100L, "application/pdf"));
    when(fx.itemMapper.insert(any(SysDocItem.class))).thenAnswer(invocation -> {
        SysDocItem item = invocation.getArgument(0);
        item.setId(88L);
        return 1;
    });
    MultipartFile file = new MockMultipartFile("file", "通知.pdf", "application/pdf", "demo".getBytes());
    DocNodeRequest request = new DocNodeRequest(2L, 5L, "通知", 30, null, false, "", null, null);

    fx.service.createFileNodeWithMainFile(10L, 2L, false, request, file);

    ArgumentCaptor<SysDocItem> itemCaptor = ArgumentCaptor.forClass(SysDocItem.class);
    verify(fx.itemMapper).insert(itemCaptor.capture());
    assertThat(itemCaptor.getValue().getDocYear()).isEqualTo(2028);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
docker run --rm -v "$PWD/backend":/workspace -v "$HOME/.m2/repository":/root/.m2/repository -w /workspace maven:3.9.9-eclipse-temurin-21 mvn -Dtest=DocWorkspaceServiceTest#createFileWithMainFileInheritsParentFolderYearWhenDocYearMissing test
```

Expected: FAIL with the existing "请选择文件年份" validation.

- [ ] **Step 3: Implement minimal service behavior**

In `DocWorkspaceService#createFileNodeWithMainFile`, resolve `docYear` from the parent folder when the request does not provide one:

```java
Integer docYear = request.docYear() == null
        ? requiredDocYear(parent.getDocYear(), "请选择文件年份")
        : requiredDocYear(request.docYear(), "请选择文件年份");
```

- [ ] **Step 4: Run the new test**

Run the same Docker Maven command. Expected: PASS.

### Task 2: 前端弹窗改为单选框并隐藏文件年份

**Files:**
- Modify: `frontend/src/views/OrgFilesView.vue`
- Modify: `docs/requirement.md`
- Modify: `docs/api.md`

- [ ] **Step 1: Update file dialog template**

Remove the file-year form item from the file dialog and replace the switch:

```vue
<el-form-item label="车间上传">
  <el-radio-group v-model="nodeForm.workshopUploadEnabled">
    <el-radio :label="false">不需要车间上传</el-radio>
    <el-radio :label="true">需要车间上传</el-radio>
  </el-radio-group>
</el-form-item>
```

- [ ] **Step 2: Keep file year derived from parent or existing item**

Keep `nodeForm.docYear` in state, but do not render it for file nodes. In `resetForm`, keep:

```ts
nodeForm.docYear = parent?.docYear || selectedYear.value || currentYear
```

In edit mode, keep the loaded item year:

```ts
nodeForm.docYear = item.docYear || node.docYear || currentYear
```

- [ ] **Step 3: Keep submission payload unchanged**

Continue appending or posting `docYear` from `nodeForm.docYear` so backend and database contracts stay stable.

- [ ] **Step 4: Update docs**

In requirements/API docs, state that the file dialog no longer exposes file-year selection and the frontend derives file year from the parent year directory.

### Task 3: Verification, merge, push, restart

**Files:**
- Verify only.

- [ ] **Step 1: Frontend build**

Run:

```bash
cd frontend && npm install && npm run build
```

Expected: build succeeds. Existing audit warnings are acceptable if unchanged.

- [ ] **Step 2: Backend tests**

Run:

```bash
docker run --rm -v "$PWD/backend":/workspace -v "$HOME/.m2/repository":/root/.m2/repository -w /workspace maven:3.9.9-eclipse-temurin-21 mvn test
```

Expected: all tests pass.

- [ ] **Step 3: Static UI checks**

Run:

```bash
rg -n "文件年份|el-switch v-model=\"nodeForm.workshopUploadEnabled\"" frontend/src/views/OrgFilesView.vue
```

Expected: no file-dialog match for the hidden file year field and no workshop-upload switch match.

- [ ] **Step 4: Merge and push**

Fetch `origin/main`, fast-forward merge the feature branch into `main`, push, then verify local and remote hashes match.

- [ ] **Step 5: Restart local services**

Restart `std-docs-backend-dev` and the `std-docs-frontend-8000` screen session, then verify `http://localhost:8000/` and `http://localhost:8000/api/auth/me`.
