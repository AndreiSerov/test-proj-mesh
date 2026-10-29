package com.example.testprojmesh.scheduler;

import com.example.testprojmesh.service.InterestService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterestSchedulerFailureTest {

    @Test
    void aFailedRunIsSwallowedSoTheNextTickStillHappens() {
        InterestService interest = mock(InterestService.class);
        when(interest.accrue()).thenThrow(new DataAccessResourceFailureException("db down")).thenReturn(3);
        InterestScheduler scheduler = new InterestScheduler(interest);

        assertThatCode(scheduler::run).doesNotThrowAnyException();
        assertThatCode(scheduler::run).doesNotThrowAnyException();

        verify(interest, times(2)).accrue();
    }
}
