package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttachmentAccessTicketServiceTest {
    @Test
    void issuedTicketCanOnlyReadBoundItemAttachment() {
        AttachmentAccessTicketService service = new AttachmentAccessTicketService(null);
        ReflectionTestUtils.setField(service, "ticketSeconds", 300L);

        String ticket = service.issueItemAttachmentTicket(new CurrentUser(5L, 9L, false), 18L);

        assertThat(ticket).isNotBlank();
        service.requireItemAttachmentTicket(18L, ticket);
        assertThatThrownBy(() -> service.requireItemAttachmentTicket(19L, ticket))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件访问票据无效或已过期");
    }
}
