package it.devchallenge.excel.controller;

import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;
import it.devchallenge.excel.dto.AddCellRequest;
import it.devchallenge.excel.dto.CellResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mariuszgromada.math.mxparser.License;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class SheetControllerTest {
    private TransitionWalker.ReachedState<RunningMongodProcess> mongo;

    @BeforeEach
    void setup() {
        License.iConfirmNonCommercialUse("testSignature");
        var mongod = Mongod.builder()
                .net(Start.to(Net.class).initializedWith(Net.defaults()
                        .withPort(27018)))
                .build();

        mongo = mongod.start(Version.Main.V6_0);
    }

    @AfterEach
    void clean() {
        mongo.close();

    }

    @Autowired
    private SheetController controller;

    @Test
    void devChallengeExampleTest() {
        // Add cells
        var res = controller.addCell("devchallenge-xx", "var1", new AddCellRequest("0"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("0");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("0");

        res = controller.addCell("devchallenge-xx", "var1", new AddCellRequest("1"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("1");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("1");

        res = controller.addCell("devchallenge-xx", "var2", new AddCellRequest("2"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("2");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("2");

        res = controller.addCell("devchallenge-xx", "var3", new AddCellRequest("=var1+var2"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("=var1+var2");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("3");

        res = controller.addCell("devchallenge-xx", "var4", new AddCellRequest("=var3+var4"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("=var3+var4");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("ERROR");

        //Get cells
        res = controller.getCell("devchallenge-xx", "var1");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("1");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("1");

        res = controller.getCell("devchallenge-xx", "var2");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("2");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("2");

        res = controller.getCell("devchallenge-xx", "var3");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("=var1+var2");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("3");

        //Get cell insensitive
        res = controller.getCell("devchallenge-XX", "VAr3");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("=var1+var2");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("3");

        //Get sheet
        res = controller.getSheet("devchallenge-xx");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        Map<String, CellResponse> body = (Map<String, CellResponse>) res.getBody();

        assertThat(body.get("var1").getValue()).isEqualTo("1");
        assertThat(body.get("var1").getResult()).isEqualTo("1");
        assertThat(body.get("var2").getValue()).isEqualTo("2");
        assertThat(body.get("var2").getResult()).isEqualTo("2");
        assertThat(body.get("var3").getValue()).isEqualTo("=var1+var2");
        assertThat(body.get("var3").getResult()).isEqualTo("3");

        //Get sheet case insensitive
        res = controller.getSheet("devchallenge-XX");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        body = (Map<String, CellResponse>) res.getBody();

        assertThat(body.get("var1").getValue()).isEqualTo("1");
        assertThat(body.get("var1").getResult()).isEqualTo("1");
        assertThat(body.get("var2").getValue()).isEqualTo("2");
        assertThat(body.get("var2").getResult()).isEqualTo("2");
        assertThat(body.get("var3").getValue()).isEqualTo("=var1+var2");
        assertThat(body.get("var3").getResult()).isEqualTo("3");
    }

    @Test
    void changeUsedInOtherCommandCellValueTest() {
        var res = controller.addCell("devchallenge-xx", "var1", new AddCellRequest("1"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("1");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("1");

        res = controller.addCell("devchallenge-xx", "var2", new AddCellRequest("2"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("2");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("2");

        res = controller.addCell("devchallenge-xx", "var3", new AddCellRequest("=var1+var2"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("=var1+var2");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("3");

        res = controller.addCell("devchallenge-xx", "var1", new AddCellRequest("2"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("2");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("2");

        res = controller.getCell("devchallenge-xx", "var3");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("=var1+var2");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("4");

        res = controller.addCell("devchallenge-xx", "var1", new AddCellRequest("=var2"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("=var2");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("2");

        res = controller.getCell("devchallenge-xx", "var3");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("=var1+var2");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("4");

        // Change insensitive
        res = controller.addCell("devchallenge-xx", "vAr1", new AddCellRequest("4"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("4");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("4");

        res = controller.getCell("devchallenge-xx", "var3");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("=var1+var2");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("6");

        res = controller.addCell("devchallenge-XX", "vAr1", new AddCellRequest("4"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("4");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("4");

        res = controller.getCell("devchallenge-xx", "var3");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("=var1+var2");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("6");
    }

    @Test
    void changeUsedInOtherCommandCellValueToIncorrectValueShouldReturnErrorTest() {
        var res = controller.addCell("devchallenge-xx", "var1", new AddCellRequest("1"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("1");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("1");

        res = controller.addCell("devchallenge-xx", "var2", new AddCellRequest("2"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("2");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("2");

        res = controller.addCell("devchallenge-xx", "var3", new AddCellRequest("=var1+var2"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("=var1+var2");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("3");

        res = controller.addCell("devchallenge-xx", "var1", new AddCellRequest("string"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("string");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("ERROR");
    }

    @Test
    void changeUsedInFormulasValueToIncompatible() {
        // Add cells
        var res = controller.addCell("devchallenge-xx", "var1", new AddCellRequest("1"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse) res.getBody()).getValue()).isEqualTo("1");
        assertThat(((CellResponse) res.getBody()).getResult()).isEqualTo("1");

        res = controller.addCell("devchallenge-xx", "var2", new AddCellRequest("2"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse) res.getBody()).getValue()).isEqualTo("2");
        assertThat(((CellResponse) res.getBody()).getResult()).isEqualTo("2");

        res = controller.addCell("devchallenge-xx", "var3", new AddCellRequest("=var1+var2"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("=var1+var2");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("3");

        res = controller.addCell("devchallenge-xx", "var4", new AddCellRequest("str"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse)res.getBody()).getValue()).isEqualTo("str");
        assertThat(((CellResponse)res.getBody()).getResult()).isEqualTo("str");

        res = controller.addCell("devchallenge-xx", "var1", new AddCellRequest("=var4"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(res.getBody()).isNotNull();
        assertThat(((CellResponse) res.getBody()).getValue()).isEqualTo("=var4");
        assertThat(((CellResponse) res.getBody()).getResult()).isEqualTo("ERROR");
    }

    @Test
    void getNotExistingCellTest() {
        var res = controller.getCell("devchallenge-xx", "var1");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody()).isNull();
    }

    @Test
    void getNotExistingSheetTest() {
        var res = controller.getSheet("devchallenge-xx");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody()).isNull();
    }
}
