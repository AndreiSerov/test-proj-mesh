package com.example.testprojmesh.api;

import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.dao.repository.TransferRepository;
import com.example.testprojmesh.support.ApiIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThat;

class TransferApiTest extends ApiIntegrationTest {

    @Autowired
    TransferRepository transfers;

    UserEntity userWith(String balance) {
        return testUsers.create("Payer", LocalDate.of(1990, 1, 1), new BigDecimal(balance));
    }

    BigDecimal balanceOf(UserEntity user) {
        String json = body(send("GET", profileUri(user), loginAs(user), null));
        return new BigDecimal(JsonPath.<Object>read(json, "$.balance").toString());
    }

    String transferJson(long toUserId, String amount) {
        return "{\"toUserId\":%d,\"amount\":%s}".formatted(toUserId, amount);
    }

    @Test
    void userSendsMoneyAndBothBalancesChangeAndTheTransferIsRecorded() {
        UserEntity payer = userWith("100.00");
        UserEntity payee = userWith("10.00");

        var result = send("POST", "/api/transfers", loginAs(payer), transferJson(payee.getId(), "25.50"));

        assertThat(result).hasStatus(201);
        String json = body(result);
        assertThat(JsonPath.<Number>read(json, "$.fromUserId").longValue()).isEqualTo(payer.getId());
        assertThat(JsonPath.<Number>read(json, "$.toUserId").longValue()).isEqualTo(payee.getId());
        assertThat(new BigDecimal(JsonPath.<Object>read(json, "$.amount").toString())).isEqualByComparingTo("25.50");
        assertThat(balanceOf(payer)).isEqualByComparingTo("74.50");
        assertThat(balanceOf(payee)).isEqualByComparingTo("35.50");
        assertThat(transfers.findById(JsonPath.<Number>read(json, "$.id").longValue())).isPresent();
    }

    @Test
    void senderIsAlwaysTheAuthenticatedUserEvenIfTheBodyNamesSomeoneElse() {
        UserEntity payer = userWith("100.00");
        UserEntity victim = userWith("100.00");
        UserEntity payee = userWith("0.01");

        var result = send("POST", "/api/transfers", loginAs(payer),
            "{\"fromUserId\":%d,\"toUserId\":%d,\"amount\":10.00}".formatted(victim.getId(), payee.getId()));

        assertThat(result).hasStatus(201);
        assertThat(balanceOf(victim)).isEqualByComparingTo("100.00");
        assertThat(balanceOf(payer)).isEqualByComparingTo("90.00");
    }

    @Test
    void insufficientFundsGivesUnprocessableAndMovesNothing() {
        UserEntity payer = userWith("10.00");
        UserEntity payee = userWith("10.00");

        var result = send("POST", "/api/transfers", loginAs(payer), transferJson(payee.getId(), "10.01"));

        assertThat(result).hasStatus(422);
        assertThat(balanceOf(payer)).isEqualByComparingTo("10.00");
        assertThat(balanceOf(payee)).isEqualByComparingTo("10.00");
    }

    @Test
    void transferToYourselfIsUnprocessable() {
        UserEntity payer = userWith("10.00");

        assertThat(send("POST", "/api/transfers", loginAs(payer), transferJson(payer.getId(), "1.00")))
            .hasStatus(422);
        assertThat(balanceOf(payer)).isEqualByComparingTo("10.00");
    }

    @Test
    void unknownRecipientIsNotFound() {
        UserEntity payer = userWith("10.00");

        assertThat(send("POST", "/api/transfers", loginAs(payer), transferJson(999_999_999L, "1.00")))
            .hasStatus(404);
        assertThat(balanceOf(payer)).isEqualByComparingTo("10.00");
    }

    @Test
    void badAmountsAndMissingFieldsAreBadRequestsWithFieldErrors() {
        UserEntity payer = userWith("10.00");
        UserEntity payee = userWith("10.00");
        String token = loginAs(payer);

        for (String amount : new String[] {"0", "-5.00", "1.234", "\"abc\"", "null"}) {
            assertThat(send("POST", "/api/transfers", token, transferJson(payee.getId(), amount)))
                .as(amount).hasStatus(400);
        }
        var missingRecipient = send("POST", "/api/transfers", token, "{\"amount\":1.00}");
        assertThat(missingRecipient).hasStatus(400);
        assertThat(body(missingRecipient)).contains("toUserId");
        assertThat(balanceOf(payer)).isEqualByComparingTo("10.00");
    }

    @Test
    void transfersRequireAuthentication() {
        assertThat(mvc.post().uri("/api/transfers").contentType("application/json")
            .content("{\"toUserId\":1,\"amount\":1.00}").exchange()).hasStatus(401);
    }
}
