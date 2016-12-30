package com.undertree.symptom.controllers;

import com.undertree.symptom.domain.Patient;
import com.undertree.symptom.domain.TestPatientBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment=RANDOM_PORT)
public class PatientControllerWebTests {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void test_PatientController_getPatient_Expect_Patient1_Exists() throws Exception {
        ResponseEntity<Patient> entity = restTemplate.getForEntity("/patients/1", Patient.class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isNotNull()
                .hasFieldOrPropertyWithValue("givenName", "Phillip")
                .hasFieldOrPropertyWithValue("familyName", "Spec")
                .hasFieldOrPropertyWithValue("birthDate", LocalDate.of(1972, 5, 5));
    }

    @Test
    public void test_PatientController_getPatient_Expect_NotFound() throws Exception {
        ResponseEntity<Patient> entity = restTemplate.getForEntity("/patients/99999999", Patient.class);
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void test_PatientController_getPatient_Expect_BadRequest() throws Exception {
        String response = restTemplate.getForObject("/patients/foo", String.class);
        assertThat(response).contains("Bad Request");
    }

    @Test
    public void test_PatientController_addPatient_Expect_OK() throws Exception {
        ResponseEntity<Patient> entity = restTemplate.postForEntity("/patients",
                new TestPatientBuilder().build(), Patient.class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isNotNull()
                .hasNoNullFieldsOrPropertiesExcept("id");
    }

    @Test
    public void test_PatientController_addPatient_WithEmpty_Expect_BadRequest() throws Exception {
        ResponseEntity<String> json = restTemplate.postForEntity("/patients", new Patient(), String.class);

        assertThat(json.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JSONAssert.assertEquals("{exception:\"org.springframework.web.bind.MethodArgumentNotValidException\"}", json.getBody(), false);
    }

    @Test
    public void test_PatientController_addPatient_WithEmptyGivenName_Expect_BadRequest() throws Exception {
        ResponseEntity<String> json = restTemplate.postForEntity("/patients",
                new TestPatientBuilder().withGivenName("").build(), String.class);

        assertThat(json.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JSONAssert.assertEquals("{exception:\"org.springframework.web.bind.MethodArgumentNotValidException\"}", json.getBody(), false);
    }

    @Test
    public void test_PatientController_addPatient_WithEmptyFamilyName_Expect_BadRequest() throws Exception {
        ResponseEntity<String> json = restTemplate.postForEntity("/patients",
                new TestPatientBuilder().withFamilyName("").build(), String.class);

        assertThat(json.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JSONAssert.assertEquals("{exception:\"org.springframework.web.bind.MethodArgumentNotValidException\"}", json.getBody(), false);
    }

    @Test
    public void test_PatientController_addPatient_WithInvalidEmail_Expect_BadRequest() throws Exception {
        ResponseEntity<String> json = restTemplate.postForEntity("/patients",
                new TestPatientBuilder().withEmail("bad/email").build(), String.class);

        assertThat(json.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JSONAssert.assertEquals("{exception:\"org.springframework.web.bind.MethodArgumentNotValidException\"}", json.getBody(), false);
    }

    @Test
    public void test_PatientController_addPatient_WithBirthDate_Expect_ValidAge() throws Exception {
        ResponseEntity<Patient> entity = restTemplate.postForEntity("/patients",
                new TestPatientBuilder().withBirthDate(LocalDate.of(1980, 1, 1)).build(), Patient.class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isNotNull()
                .hasFieldOrPropertyWithValue("age", LocalDate.now().getYear() - 1980);
    }

    @Test
    public void test_PatientController_updatePatient_WithValidRandom_Expect_OK() throws Exception {
        HttpEntity<Patient> patientToUpdate = new HttpEntity<>(new TestPatientBuilder().build());

        ResponseEntity<Patient> entity = restTemplate.exchange("/patients/{id}", HttpMethod.PUT,
                patientToUpdate, Patient.class, 1L);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isNotNull()
                .isEqualToIgnoringGivenFields(patientToUpdate.getBody(), "id");
    }

    @Test
    public void test_PatientController_patchPatient_TODO() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1L);

        Patient patchPatient = new Patient();
        patchPatient.setEmail("somewhere@overtherainbow.com");
        HttpEntity<Patient> patientToUpdate = new HttpEntity<>(patchPatient);

        // great the default impl doesn't appear to support PATCH...
        // this appears to have been fixed in 1.4.3: https://github.com/spring-projects/spring-boot/issues/7412
        // but still won't work until 1.4.4: https://github.com/spring-projects/spring-boot/issues/7742

        //ResponseEntity<Patient> entity = restTemplate.exchange("/patient/{id}", HttpMethod.PATCH,
        //        patientToUpdate, Patient.class, params);

        //assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        //assertThat(entity.getBody()).isNotNull()
        //        .isEqualToIgnoringGivenFields(patientToUpdate.getBody(), "id");
    }

    @Test
    public void test_PatientController_getPatients_Expect_OK() throws Exception {
        ResponseEntity<Patient[]> entity = restTemplate.getForEntity("/patients", Patient[].class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(Arrays.asList(entity.getBody())).isNotNull()
                .extracting(Patient::getFamilyName)
                .contains("Spec", "Certify", "Neubus");
    }

    @Test
    public void test_PatientController_getPatientsWithPagination_Expect_PagedResult() throws Exception {
        ResponseEntity<Patient[]> entity =
                restTemplate.getForEntity("/patients?page={page}&size={size}", Patient[].class, 1, 1);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getHeaders().containsKey("X-Meta-Pagination")).isTrue();

        assertThat(Arrays.asList(entity.getBody())).isNotNull()
                .extracting(Patient::getFamilyName)
                .containsOnly("Certify");
    }

    @Test
    public void test_PatientController_getPatientsWithSortDesc_Expect_OrderedResult() throws Exception {
        ResponseEntity<Patient[]> entity =
                restTemplate.getForEntity("/patients?sort=familyName,desc", Patient[].class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getHeaders().containsKey("X-Meta-Pagination")).isTrue();

        assertThat(Arrays.asList(entity.getBody())).isNotNull()
                .extracting(Patient::getFamilyName)
                .contains("Spec", "Neubus", "Certify");
    }
}
