SET NAMES utf8mb4;

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_image_doc_categories (
  section_name VARCHAR(128) NOT NULL,
  category_name VARCHAR(128) NOT NULL,
  sort_order INT NOT NULL
) ENGINE=Memory DEFAULT CHARSET=utf8mb4;

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_image_doc_items (
  section_name VARCHAR(128) NOT NULL,
  category_name VARCHAR(128) NOT NULL,
  item_name VARCHAR(128) NOT NULL,
  sort_order INT NOT NULL
) ENGINE=Memory DEFAULT CHARSET=utf8mb4;

TRUNCATE TABLE tmp_image_doc_categories;
TRUNCATE TABLE tmp_image_doc_items;

INSERT INTO tmp_image_doc_categories (section_name, category_name, sort_order) VALUES
('技术科', '技术规范', 10),
('技术科', '技术文件', 20),
('技术科', '房建设备检修记录', 30),
('技术科', '专项检查', 40),
('技术科', '基础台账', 50),
('技术科', '专项台账', 60),
('技术科', '限界管理', 70),
('技术科', '客运车站厕所', 80),
('技术科', '防洪工作', 90),
('技术科', '房建维修', 100),
('技术科', '房建大修', 110),
('技术科', '供热春检秋检', 120),
('技术科', '特种设备', 130),
('技术科', '供热生产管理', 140),
('技术科', '供热综合维修', 150),
('计划财务科', '财务管理', 10),
('劳动人事科（党委组织科）', '劳人管理文件', 10),
('劳动人事科（党委组织科）', '人事命令存档', 20),
('劳动人事科（党委组织科）', '劳资管理资料', 30);

INSERT INTO tmp_image_doc_items (section_name, category_name, item_name, sort_order) VALUES
('技术科', '技术规范', '国铁集团技术规章', 10),
('技术科', '技术规范', '集团公司级规章', 20),
('技术科', '技术规范', '段级技术规章', 30),
('技术科', '技术文件', '集团公司级、段级有效技术文件目录及要求配备的纸质文档', 10),
('技术科', '技术文件', '集团公司级技术文件', 20),
('技术科', '技术文件', '段级技术文件', 30),
('技术科', '房建设备检修记录', '房屋建筑物检修验收记录', 10),
('技术科', '房建设备检修记录', '房建设备病害问题库', 20),
('技术科', '专项检查', '上级各类专项检查、专项排查通知文件及相关检查落实情况等', 10),
('技术科', '基础台账', '房屋设备表（建维修6）', 10),
('技术科', '基础台账', '构筑物设备表（建维修6-1）', 20),
('技术科', '基础台账', '室外公共构筑物设备表（建维修7）', 30),
('技术科', '基础台账', '照明设备表（建维修8）', 40),
('技术科', '基础台账', '室内水道卫生器具设备表（建维修9）', 50),
('技术科', '基础台账', '采暖锅炉设备表（建维修10）', 60),
('技术科', '基础台账', '室内公共设施设备表（建维修10-1）', 70),
('技术科', '基础台账', '站台技术状态调查表（建维修13-1）', 80),
('技术科', '基础台账', '雨棚技术状态调查表（建维修13-2）', 90),
('技术科', '基础台账', '宗地图', 100),
('技术科', '专项台账', '房建车间管辖线别及营业里程统计表', 10),
('技术科', '专项台账', '车站统计表', 20),
('技术科', '专项台账', '站房雨棚特殊设备情况统计表', 30),
('技术科', '专项台账', '站房雨棚电伴热融雪系统统计表', 40),
('技术科', '专项台账', '彩钢屋面设备台账', 50),
('技术科', '专项台账', '客运车站厕所统计表', 60),
('技术科', '专项台账', '房屋、构筑物结构鉴定明细表', 70),
('技术科', '专项台账', '房屋构筑物防雷设施检测记录', 80),
('技术科', '专项台账', '轨旁设备汇总表', 90),
('技术科', '专项台账', '高大门窗台账', 100),
('技术科', '专项台账', '照明配电箱设备台账', 110),
('技术科', '专项台账', '动力配电柜（箱）设备台账', 120),
('技术科', '专项台账', '风电动力设备台账', 130),
('技术科', '限界管理', '站台、雨棚限界信息记录（太房专-007）', 10),
('技术科', '限界管理', '段限设施设备统计表', 20),
('技术科', '限界管理', '超限货物运输路径限界核查资料', 30),
('技术科', '客运车站厕所', '客运车站厕所设施设备台账及平面示意图', 10),
('技术科', '防洪工作', '上级防洪文件（集团公司级、段级）', 10),
('技术科', '防洪工作', '车间防洪工作安排', 20),
('技术科', '防洪工作', '防洪应急预案', 30),
('技术科', '防洪工作', '防洪隐患排查相关记录', 40),
('技术科', '防洪工作', '防洪处置情况统计表', 50),
('技术科', '房建维修', '合同（附合同联签表）', 10),
('技术科', '房建维修', '安全协议', 20),
('技术科', '房建维修', '开工报告', 30),
('技术科', '房建维修', '项目验收签认表', 40),
('技术科', '房建维修', '竣工验收报告', 50),
('技术科', '房建维修', '施工前、中、后的影像资料', 60),
('技术科', '房建维修', '工程洽商、联系单', 70),
('技术科', '房建维修', '技术交底', 80),
('技术科', '房建维修', '隐蔽工程验收记录', 90),
('技术科', '房建维修', '主要工程材料合格证、材料试验报告', 100),
('技术科', '房建维修', '房建设备维修验收记录', 110),
('技术科', '房建维修', '房建设备计划检修（完成）统计表', 120),
('技术科', '房建维修', '房建设备综合维修计划（完成）统计表', 130),
('技术科', '房建大修', '大修项目提报计划资料', 10),
('技术科', '房建大修', '大修项目计划文件', 20),
('技术科', '房建大修', '审查通知书', 30),
('技术科', '房建大修', '施工合同', 40),
('技术科', '房建大修', '安全协议', 50),
('技术科', '房建大修', '施工图及预算', 60),
('技术科', '房建大修', '开工报告', 70),
('技术科', '房建大修', '技术交底', 80),
('技术科', '房建大修', '施工组织方案', 90),
('技术科', '房建大修', '施工材料质量验收单', 100),
('技术科', '房建大修', '隐蔽工程验收记录', 110),
('技术科', '房建大修', '大修分项工程竣工验收记录', 120),
('技术科', '房建大修', '项目验收签认表', 130),
('技术科', '房建大修', '工程竣工验收报告', 140),
('技术科', '房建大修', '大修前、后对比照片和隐蔽工程照片', 150),
('技术科', '房建大修', '计价及结算资料', 160),
('技术科', '房建大修', '设计变更资料', 170),
('技术科', '房建大修', '转固及其他资料', 180),
('技术科', '供热春检秋检', '段发春检、秋检文件', 10),
('技术科', '供热春检秋检', '车间春检、秋检安排', 20),
('技术科', '供热春检秋检', '文电要求的各项检查统计表', 30),
('技术科', '供热春检秋检', '车间各项台账和图纸', 40),
('技术科', '供热春检秋检', '特种设备统计台账、设备取历簿、建立一台一档等资料', 50),
('技术科', '特种设备', '安全附件及仪器仪表台账', 10),
('技术科', '特种设备', '特种设备使用登记证', 20),
('技术科', '特种设备', '特种设备定期检验报告（锅检报告）', 30),
('技术科', '特种设备', '能效测试报告（锅炉）', 40),
('技术科', '特种设备', '机械动力设备台账', 50),
('技术科', '特种设备', '其他相关资料（如设备调拨、报废等原始资料）', 60),
('技术科', '供热生产管理', '整修计划、查勘单以及相关验收记录', 10),
('技术科', '供热生产管理', '各类专项检查资料', 20),
('技术科', '供热综合维修', '合同（附合同联签表）', 10),
('技术科', '供热综合维修', '安全协议', 20),
('技术科', '供热综合维修', '开工报告', 30),
('技术科', '供热综合维修', '竣工验收报告', 40),
('技术科', '供热综合维修', '施工前、中、后的影像资料', 50),
('计划财务科', '财务管理', '固定资产明细表，固定资产台账需与技术台账对应', 10),
('计划财务科', '财务管理', '资产清查记录', 20),
('计划财务科', '财务管理', '收款二维码及音响保管台账，收据申领登记簿、交费记录登记簿', 30),
('计划财务科', '财务管理', '“小金库”承诺书、记名传达文件等其他资料', 40),
('劳动人事科（党委组织科）', '劳人管理文件', '上级各种文件、通知、通报和记名传达记录表等', 10),
('劳动人事科（党委组织科）', '人事命令存档', '需车间留存的人事命令', 10),
('劳动人事科（党委组织科）', '劳资管理资料', '车间绩效考核办法', 10),
('劳动人事科（党委组织科）', '劳资管理资料', '车间月度考核分配表', 20),
('劳动人事科（党委组织科）', '劳资管理资料', '车间月度考勤表、各类请假记录', 30);

INSERT INTO sys_doc_category (
  section_dept_id,
  category_name,
  sort_order,
  status,
  created_at,
  updated_at,
  deleted
)
SELECT
  d.id,
  c.category_name,
  c.sort_order,
  'ENABLED',
  NOW(),
  NOW(),
  0
FROM tmp_image_doc_categories c
JOIN sys_dept d
  ON d.dept_name = c.section_name
 AND d.deleted = 0
 AND d.status = 'ENABLED'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_doc_category existing
  WHERE existing.section_dept_id = d.id
    AND existing.category_name = c.category_name
    AND existing.deleted = 0
);

UPDATE sys_doc_category category
JOIN sys_dept d
  ON d.id = category.section_dept_id
 AND d.deleted = 0
 AND d.status = 'ENABLED'
JOIN tmp_image_doc_categories seed
  ON seed.section_name = d.dept_name
 AND seed.category_name = category.category_name
SET category.sort_order = seed.sort_order,
    category.status = 'ENABLED',
    category.updated_at = NOW()
WHERE category.deleted = 0;

INSERT INTO sys_doc_item (
  category_id,
  item_name,
  content_html,
  collect_enabled,
  attachment_enabled,
  attachment_required,
  sort_order,
  status,
  created_at,
  updated_at,
  deleted
)
SELECT
  category.id,
  seed.item_name,
  '',
  1,
  1,
  0,
  seed.sort_order,
  'ENABLED',
  NOW(),
  NOW(),
  0
FROM tmp_image_doc_items seed
JOIN sys_dept d
  ON d.dept_name = seed.section_name
 AND d.deleted = 0
 AND d.status = 'ENABLED'
JOIN sys_doc_category category
  ON category.section_dept_id = d.id
 AND category.category_name = seed.category_name
 AND category.deleted = 0
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_doc_item existing
  WHERE existing.category_id = category.id
    AND existing.item_name = seed.item_name
    AND existing.deleted = 0
);

UPDATE sys_doc_item item
JOIN sys_doc_category category
  ON category.id = item.category_id
 AND category.deleted = 0
JOIN sys_dept d
  ON d.id = category.section_dept_id
 AND d.deleted = 0
 AND d.status = 'ENABLED'
JOIN tmp_image_doc_items seed
  ON seed.section_name = d.dept_name
 AND seed.category_name = category.category_name
 AND seed.item_name = item.item_name
SET item.attachment_enabled = 1,
    item.collect_enabled = 1,
    item.attachment_required = 0,
    item.sort_order = seed.sort_order,
    item.status = 'ENABLED',
    item.updated_at = NOW()
WHERE item.deleted = 0;

SELECT
  seed.section_name,
  COUNT(DISTINCT category.id) AS category_count,
  COUNT(item.id) AS item_count
FROM tmp_image_doc_categories seed
JOIN sys_dept d
  ON d.dept_name = seed.section_name
 AND d.deleted = 0
 AND d.status = 'ENABLED'
LEFT JOIN sys_doc_category category
  ON category.section_dept_id = d.id
 AND category.category_name = seed.category_name
 AND category.deleted = 0
LEFT JOIN sys_doc_item item
  ON item.category_id = category.id
 AND item.deleted = 0
WHERE seed.section_name IN ('技术科', '计划财务科', '劳动人事科（党委组织科）')
GROUP BY seed.section_name
ORDER BY seed.section_name;
