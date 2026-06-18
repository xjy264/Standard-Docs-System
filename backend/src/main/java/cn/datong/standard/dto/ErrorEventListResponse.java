package cn.datong.standard.dto;

import cn.datong.standard.entity.SysErrorEvent;

import java.util.List;

public record ErrorEventListResponse(
        List<SysErrorEvent> rows,
        long total,
        List<ErrorEventStat> stats
) {
}
