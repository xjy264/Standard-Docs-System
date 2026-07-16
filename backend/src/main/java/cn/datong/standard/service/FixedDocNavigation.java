package cn.datong.standard.service;

import cn.datong.standard.entity.SysDept;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FixedDocNavigation {
    public static final String DOC_SECTION = "DOC_SECTION";

    private static final List<String> INTERNAL_CODES = List.of(
            "OFFICE",
            "SAFETY",
            "FINANCE",
            "APARTMENT_SECTION",
            "PARTY_AFFAIRS",
            "HR_ORG",
            "TECH",
            "BUILDING_MAINTENANCE_CENTER",
            "PRODUCTION_DISPATCH_CENTER"
    );
    private static final List<String> RULES_CODES = List.of(
            "DOC_TECH_RULES",
            "DOC_TECH_FILES",
            "DOC_MANAGEMENT_METHODS",
            "OFFICE",
            "SAFETY",
            "FINANCE",
            "APARTMENT_SECTION",
            "PARTY_AFFAIRS",
            "HR_ORG",
            "TECH",
            "BUILDING_MAINTENANCE_CENTER",
            "PRODUCTION_DISPATCH_CENTER"
    );
    private static final Set<String> FIXED_CODES = Set.copyOf(RULES_CODES);
    private static final Set<String> FIXED_NAMES = Set.of(
            "技术规章",
            "技术文件",
            "管理办法",
            "办公室（党委办公室）",
            "安全培训科",
            "计划财务科",
            "公寓科",
            "党群工作科",
            "劳动人事科（党委组织科）",
            "技术科",
            "房建监测和维修中心",
            "生产调度和监控中心"
    );

    private FixedDocNavigation() {
    }

    public static List<SysDept> ordered(List<SysDept> depts, String moduleType) {
        Map<String, SysDept> byCode = new LinkedHashMap<>();
        for (SysDept dept : depts) {
            if (dept.getDeptCode() != null) {
                byCode.putIfAbsent(dept.getDeptCode(), dept);
            }
        }
        List<String> codes = "RULES".equalsIgnoreCase(moduleType) ? RULES_CODES : INTERNAL_CODES;
        return codes.stream().map(byCode::get).filter(java.util.Objects::nonNull).toList();
    }

    public static boolean isFixed(SysDept dept) {
        return dept != null && isFixedCode(dept.getDeptCode());
    }

    public static boolean isFixedCode(String code) {
        return code != null && FIXED_CODES.contains(code);
    }

    public static boolean isFixedName(String name) {
        return name != null && FIXED_NAMES.contains(name.trim());
    }

    public static boolean isDocSection(SysDept dept) {
        return dept != null && DOC_SECTION.equalsIgnoreCase(dept.getDeptType());
    }
}
