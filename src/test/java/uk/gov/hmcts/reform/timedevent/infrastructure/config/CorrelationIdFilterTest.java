package uk.gov.hmcts.reform.timedevent.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private CorrelationIdFilter correlationIdFilter;

    @BeforeEach
    void setUp() {
        correlationIdFilter = new CorrelationIdFilter();
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void should_use_correlation_id_from_header_when_present() throws Exception {
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn("existing-correlation-id");

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "existing-correlation-id");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void should_generate_correlation_id_when_header_not_present() throws Exception {
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(null);

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), argThat(id -> id != null && !id.isBlank()));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void should_generate_correlation_id_when_header_is_blank() throws Exception {
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn("   ");

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), argThat(id -> id != null && !id.isBlank()));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void should_clear_mdc_after_filter_execution() throws Exception {
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn("test-id");

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
        assertNull(MDC.get(CorrelationIdFilter.CCD_CASE_ID_MDC_KEY));
    }

    @Test
    void should_clear_mdc_even_when_exception_occurs() throws Exception {
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn("test-id");
        doThrow(new RuntimeException("Test exception")).when(filterChain).doFilter(request, response);

        assertThrows(RuntimeException.class, () ->
            correlationIdFilter.doFilterInternal(request, response, filterChain)
        );

        assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
    }

    @Test
    void getCorrelationId_should_return_value_from_mdc() {
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, "mdc-correlation-id");

        assertEquals("mdc-correlation-id", CorrelationIdFilter.getCorrelationId());
    }

    @Test
    void setCcdCaseId_should_set_value_in_mdc() {
        CorrelationIdFilter.setCcdCaseId("12345");

        assertEquals("12345", MDC.get(CorrelationIdFilter.CCD_CASE_ID_MDC_KEY));
    }

    @Test
    void setCcdCaseId_should_not_set_null_value() {
        CorrelationIdFilter.setCcdCaseId(null);

        assertNull(MDC.get(CorrelationIdFilter.CCD_CASE_ID_MDC_KEY));
    }
}
