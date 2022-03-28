package com.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.Api;
import com.model.ApiField;
import com.model.CommonApi;
import com.model.CorporateField;
import com.model.CorporateUser;
import com.model.EverywhereRemit;
import com.model.FinanceNow;
import com.model.PaymentGo;
import com.model.SelectedField;
import com.repository.ApiFieldRepository;
import com.repository.ApiRepository;
import com.repository.CorporateFieldRepository;
import com.repository.CorporateUserRepository;
import com.repository.SelectedFieldRepository;
import com.request.FieldMapRequest;
import com.request.SendTransaction;
import com.request.TransactionAuth;
import com.response.TransactOutcome;
import com.response.TransactResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin(origins = "http://localhost:8081")
@RestController
@RequestMapping("/api")
public class FieldMappingController {
    private final String transactionToken = "UGdsCTmT3AEWmngHyJg9OoWxwSl8Z4";
    @Autowired
    private ApiRepository apiRepository;
    @Autowired
    private ApiFieldRepository apiFieldRepository;
    @Autowired
    private CorporateFieldRepository corporateFieldRepository;
    @Autowired
    private CorporateUserRepository corporateUserRepository;
    @Autowired
    private SelectedFieldRepository selectedFieldRepository;

    public Api determineApi(List<Api> apiList, double amount) {
        Api searchApi = null;
        Iterator<Api> iterApi = apiList.iterator();
        while (iterApi.hasNext()) {
            Api currentApi = iterApi.next();
            if (amount > currentApi.getMinAmount() && amount <= currentApi.getMaxAmount()) {
                searchApi = apiRepository.findById(currentApi.getApiId()).orElseThrow(() 
                    -> new ResourceNotFoundException("No Api found with api_id = " + currentApi.getApiId()));
                return searchApi;
            }
        }
        return searchApi;
    }

    // Returns "" if there is not error
    public String checkDataType(Cell column, ApiField apiField) {
        String errorMessage = "";
        String dataType = apiField.getDatatype();
        List<SelectedField> selectedFields = selectedFieldRepository.findAllSelectedByApiFieldId(apiField);

        // Check if the field is a selected field
        if (selectedFields.isEmpty()) {
            // Validation: Return true if column.toString() is a String (Regex Validation - only alphanumeric and symbols)
            if (dataType.equals("String")) {
                // code goes here: 
                

                return errorMessage;
            } 
            // Validation: Return true if column.toString() is able to parse into an Int/Double
            else if (dataType.equals("Number")) {
                // code goes here: 
    
    
                return errorMessage;
            } 
            // Validation: Return true if column.toString() is able to parse into DateTime
            else if (dataType.equals("Date")) {
                // code goes here: 
    
                
                return errorMessage;
            }
        } else {
            // Iterator<SelectedField> iterSelectedField = selectedFields.iterator();
            // Validation: Check if column.toString() is inside selectedFields

            return errorMessage;
            // To be completed
            // while (iterSelectedField.hasNext()) {
            //     SelectedField currentIterSelected = iterSelectedField.next();
            //     if (currentIterSelected.getSelectedFieldCode().equals(column.toString())) {
            //         return true;
            //     } 
            // }
        }
        return errorMessage;
    }

    // Can call this method if new access token is needed for a transaction
    public String authSandbox() {
        String url = "https://prelive.paywho.com/api/smu_authenticate";
        RestTemplate restTemplate = new RestTemplate();
        TransactionAuth credentials = new TransactionAuth("test", "123456");

        HttpEntity<TransactionAuth> requestEntity = new HttpEntity<>(credentials);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);
        String responseString = responseEntity.getBody();
        String returnMessage = "";

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseString);
            JsonNode innerNode = rootNode.get("access_token");
            returnMessage = innerNode.asText();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return returnMessage;
    }

    @PostMapping("/uploadFieldMapping/{corporateUserId}/{amountCol}")
    public ResponseEntity<TransactResponse> addFieldMapping(
        @PathVariable("corporateUserId") long corporateUserId, 
        @PathVariable("amountCol") int amountCol, 
        @RequestParam("file") MultipartFile file) {
        
        List<TransactOutcome> transactOutcomeList = new ArrayList<>();
        List<CommonApi> commonApiList = new ArrayList<>();
        List<String> errorList = new ArrayList<>();

        CorporateUser corporateUser = corporateUserRepository.findById(corporateUserId).orElseThrow(() 
            -> new ResourceNotFoundException("No Corporate found with corporate_id = " + corporateUserId));
        List<CorporateField> corporateFields = corporateFieldRepository.findAllCorpFieldByUserId(corporateUser);

        // Generate CorporateField HashMap
        Iterator<CorporateField> iterCorpField = corporateFields.iterator();
        HashMap<String, Set<ApiField>> fieldMapping = new HashMap<>();
        while (iterCorpField.hasNext()) {
            CorporateField currentCorpField = iterCorpField.next();
            Set<ApiField> currentApiFields = currentCorpField.getApiFields();
            fieldMapping.put(currentCorpField.getCorporateFieldName(), currentApiFields);
        }
        try {
            Workbook workbook = new XSSFWorkbook(file.getInputStream());
            // To be changed
            int headerRow = 1;
            Sheet sh = workbook.getSheetAt(0);
            Row header = sh.getRow(headerRow);
            List<Api> apiList = apiRepository.findAll();
            Iterator<Row> iterApiRow = sh.iterator();
            // Skipping headers row
            for (int i=0; i<headerRow; i++) {
                iterApiRow.next();
            }
            while (iterApiRow.hasNext()) {
                try {
                    int colCounter = 1;
                    Row currentRow = iterApiRow.next();
                    int rowNum = currentRow.getRowNum();
                    CommonApi commonApi = new CommonApi();
                    String errorMessage = "";

                    // Determine which API to instantiate based on the amount
                    Double amount = Double.parseDouble(currentRow.getCell(amountCol - 1).toString());
                    Api searchApi = determineApi(apiList, amount);
                    // Amount matches an API range 
                    if (searchApi != null) {
                        String testApiName = searchApi.getApiName();
                        if (testApiName.equals("FinanceNow")) {
                            commonApi = new FinanceNow();
                        } else if (testApiName.equals("EverywhereRemit")) {
                            commonApi = new EverywhereRemit();
                        } else if (testApiName.equals("PaymentGo")) {
                            commonApi = new PaymentGo();
                        }
                        // Retrieving the row's cell value
                        Iterator<Cell> iterApiCol = currentRow.iterator();
                        Iterator<Cell> iterHeadGet = header.iterator();
                        while (iterApiCol.hasNext()) {
                            Cell currentCol = iterApiCol.next();
                            Cell currentHeader = iterHeadGet.next();
                            // Concatenates Excel's column header with colCounter to retrieve mapped fields
                            String searchApiField = currentHeader.toString() + "_" + String.valueOf(colCounter++);
                            Set<ApiField> apiFields = fieldMapping.get(searchApiField);
                            // Current header is matched to an API Field
                            if (apiFields != null) {
                                for (ApiField apiField : apiFields) {
                                    // Calls checkDataType method to perform data validation
                                    String validationOutput = checkDataType(currentCol, apiField);
                                    if (validationOutput.equals("")) {
                                        String apiFieldName = apiField.getApiFieldName();
                                        commonApi.apiSetter(currentCol.toString(), apiFieldName);
                                    }
                                    // Column has failed data validation
                                    else {
                                        // Validation: 
                                        errorMessage += validationOutput;
                                    }
                                }
                            }
                        }
                        if (errorMessage.equals("")) {
                            commonApiList.add(commonApi);
                        } else {
                            errorList.add(String.format("Error row%s: %s", String.valueOf(rowNum), errorMessage));
                        }
                    }
                    // Amount is not within the range
                    else {
                        // Validation: 
                        errorList.add(String.format("Error row%s: %s", String.valueOf(rowNum), "Amount is not within API range"));
                    }
                // amount column has a non-number value
                } catch (NumberFormatException e) {
                    // Validation: 
                    
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Fail upload transactions: " + e.getMessage());
        }
        if (errorList.size() == 0) {
            transactOutcomeList = uploadAllTransactions(commonApiList);
        }
        TransactResponse TransactResponse = new TransactResponse(transactOutcomeList, errorList);
        return new ResponseEntity<>(TransactResponse, HttpStatus.CREATED);
    }

    public List<TransactOutcome> uploadAllTransactions(List<CommonApi> commonApiList) {
        String url = "https://prelive.paywho.com/api/smu_send_transaction";
        RestTemplate restTemplate = new RestTemplate();
        // Seconds of delay per API call
        int apiDelay = 0;
        List<TransactOutcome> transactOutcomeList = new ArrayList<>();

        for (CommonApi commonApi : commonApiList) {
            String apiName = "";
            if (commonApi instanceof FinanceNow) {
                apiName = "financenow";
            }
            else if (commonApi instanceof EverywhereRemit) {
                apiName = "everywhereremit";
            } 
            else if (commonApi instanceof PaymentGo) {
                apiName = "paymentgo";
            }
            SendTransaction credentials = new SendTransaction(transactionToken, apiName, commonApi);
            HttpEntity<SendTransaction> requestEntity = new HttpEntity<>(credentials);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(responseEntity.getBody());
                JsonNode innerNode = rootNode.get("message");
                transactOutcomeList.add(new TransactOutcome(commonApi, innerNode.asText(), apiName));
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            try {
                TimeUnit.SECONDS.sleep(apiDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return transactOutcomeList;
    }

    @PostMapping("/addFieldMapping")
    public ResponseEntity<List<ApiField>> addFieldMapping(@RequestBody List<FieldMapRequest> fieldMapRequests) {
        List<ApiField> apiFieldList = new ArrayList<ApiField>();
        for (FieldMapRequest fieldMapRequest : fieldMapRequests) {
            long corporateFieldId = fieldMapRequest.getCorporateFieldId();
            long apiFieldId = fieldMapRequest.getApiFieldId();
            CorporateField corporateField = corporateFieldRepository.findById(apiFieldId).orElseThrow(() 
                -> new ResourceNotFoundException("No Api found with corporate_field_id = " + corporateFieldId));
            ApiField apiField = apiFieldRepository.findById(apiFieldId).orElseThrow(() 
                -> new ResourceNotFoundException("No Api found with api_field_id = " + apiFieldId));
            corporateFieldRepository.save(corporateField);
            apiFieldList.add(apiFieldRepository.save(apiField));
        }
        return new ResponseEntity<>(apiFieldList, HttpStatus.CREATED);
    }

    // @PostMapping("/addFieldMappingOld/{corporateFieldId}")
    // public ResponseEntity<ApiField> addFieldMappingOld(
    //     @PathVariable(value = "corporateFieldId") Long corporateFieldId, 
    //     @RequestBody ApiField apiFieldRequest) {

    //     ApiField apiField = corporateFieldRepository.findById(corporateFieldId).map(corporateField -> {
    //         long apiFieldId = apiFieldRequest.getApiFieldId();
    //         // ApiField exists
    //         if (apiFieldId != 0L) {
    //             ApiField _apiField = apiFieldRepository.findById(apiFieldId)
    //                 .orElseThrow(() -> new ResourceNotFoundException("No Api Field found with api_field_id = " + apiFieldId));
    //             corporateField.addApiField(_apiField);
    //             corporateFieldRepository.save(corporateField);
    //             return _apiField;
    //         }
    //         // Add ApiField 
    //         corporateField.addApiField(apiFieldRequest);
    //         return apiFieldRepository.save(apiFieldRequest);
    //     }).orElseThrow(() -> new ResourceNotFoundException("No Corporate Field found with corporate_field_id = " + corporateFieldId));
    //     return new ResponseEntity<>(apiField, HttpStatus.CREATED);
    // }
}
