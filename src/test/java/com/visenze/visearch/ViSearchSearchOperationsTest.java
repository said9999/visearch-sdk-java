package com.visenze.visearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.visenze.visearch.internal.SearchOperations;
import com.visenze.visearch.internal.SearchOperationsImpl;
import com.visenze.visearch.internal.http.ViSearchHttpClient;
import com.visenze.visearch.internal.http.ViSearchHttpResponse;
import com.visenze.visearch.internal.json.ViSearchModule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class ViSearchSearchOperationsTest {

    private ViSearchHttpClient mockClient;
    private ObjectMapper objectMapper;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void beforeTest() {
        mockClient = mock(ViSearchHttpClient.class);
        objectMapper = new ObjectMapper().registerModule(new ViSearchModule());
    }

    @Test
    public void testSearchParamsBasic() {
        String responseBody = "{\"status\":\"OK\",\"method\":\"search\",\"error\":[],\"page\":1,\"limit\":10,\"total\":20,\"result\":[{\"im_name\":\"test_im_1\"}]}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        Map<String, String> responseHeaders = Maps.newHashMap();
        responseHeaders.put("test-param", "123");
        when(response.getHeaders()).thenReturn(responseHeaders);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        SearchParams searchParams = new SearchParams("test_im");
        PagedSearchResult pagedSearchResult = searchOperations.search(searchParams);
        assertEquals(responseHeaders, pagedSearchResult.getHeaders());
        assertEquals("test_im", searchParams.getImName());
        assertEquals(null, pagedSearchResult.getErrorMessage());
        assertEquals(null, pagedSearchResult.getCause());
        assertEquals(null, pagedSearchResult.getRawResponseMessage());
        Multimap<String, String> expectedParams = HashMultimap.create();
        expectedParams.put("im_name", "test_im");
        expectedParams.put("score", "false");
        verify(mockClient).get("/search", expectedParams);
    }

    @Test
    public void testSearchParamsFacet() {
        String responseBody = "{\"status\":\"OK\",\"method\":\"search\",\"error\":[],\"page\":1,\"limit\":10,\"total\":20,\"result\":[{\"im_name\":\"test_im_1\"}],\"facets\":[{\"key\":\"brand\",\"items\":[{\"value\":\"brandA\",\"count\":5},{\"value\":\"brandB\",\"count\":6},{\"value\":\"brandC\",\"count\":9}]}]}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        SearchParams searchParams = new SearchParams("test_im")
                .setFacets(Lists.newArrayList("brand"))
                .setFacetsLimit(10)
                .setFacetsShowCount(true);
        searchOperations.search(searchParams);
        Multimap<String, String> expectedParams = HashMultimap.create();
        expectedParams.put("im_name", "test_im");
        expectedParams.put("facets", "brand");
        expectedParams.put("facets_limit", "10");
        expectedParams.put("facets_show_count", "true");
        expectedParams.put("score", "false");
        verify(mockClient).get("/search", expectedParams);
    }

    @Test
    public void testSearchParamsFacetOnNumber() {
        String responseBody = "{\"status\":\"OK\",\"method\":\"search\",\"error\":[],\"page\":1,\"limit\":10,\"total\":20,\"result\":[{\"im_name\":\"test_im_1\"}],\"facets\":[{\"key\":\"brand\",\"range\":{\"min\":1, \"max\":420}}]}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        SearchParams searchParams = new SearchParams("test_im")
                .setFacets(Lists.newArrayList("brand"))
                .setFacetsLimit(10)
                .setFacetsShowCount(true);
        PagedSearchResult searchResult = searchOperations.search(searchParams);
        Multimap<String, String> expectedParams = HashMultimap.create();
        expectedParams.put("im_name", "test_im");
        expectedParams.put("facets", "brand");
        expectedParams.put("facets_limit", "10");
        expectedParams.put("facets_show_count", "true");
        expectedParams.put("score", "false");
        verify(mockClient).get("/search", expectedParams);
        List<Facet> facets = searchResult.getFacets();
        assertEquals(1, facets.size());
        assertEquals(1, facets.get(0).getRange().getMin());
        assertEquals(420, facets.get(0).getRange().getMax());
    }

    @Test
    public void testSearchParamsFull() {
        String responseBody = "{\"status\":\"OK\",\"method\":\"search\",\"error\":[],\"page\":10,\"limit\":1,\"total\":20,\"result\":[{\"im_name\":\"test_im_1\"}]}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        Map<String, String> fq = Maps.newHashMap();
        fq.put("field_a", "value_a");
        fq.put("field_b", "value_b");
        Map<String, String> custom = Maps.newHashMap();
        custom.put("custom_key", "custom_value");
        SearchParams searchParams = new SearchParams("test_im")
                .setPage(10)
                .setLimit(1)
                .setScore(true)
                .setScoreMin(0.1f)
                .setScoreMax(0.75f)
                .setFq(fq)
                .setFl(Lists.newArrayList("field_x", "field_y"))
                .setGetAllFl(true)
                .setQInfo(true)
                .setCustom(custom);
        searchOperations.search(searchParams);
        Multimap<String, String> expectedParams = HashMultimap.create();
        expectedParams.put("im_name", "test_im");
        expectedParams.put("page", "10");
        expectedParams.put("limit", "1");
        expectedParams.put("score", "true");
        expectedParams.put("score_min", "0.1");
        expectedParams.put("score_max", "0.75");
        expectedParams.put("fq", "field_a:value_a");
        expectedParams.put("fq", "field_b:value_b");
        expectedParams.put("fl", "field_x");
        expectedParams.put("fl", "field_y");
        expectedParams.put("get_all_fl", "true");
        expectedParams.put("qinfo", "true");
        expectedParams.put("custom_key", "custom_value");
        verify(mockClient).get("/search", expectedParams);
    }

    @Test
    public void testSearchResponseBasic() {
        String responseBody = "{\"status\":\"OK\",\"method\":\"search\",\"error\":[],\"page\":1,\"limit\":10,\"total\":20,\"result\":[{\"im_name\":\"test_im_0\"},{\"im_name\":\"test_im_1\"},{\"im_name\":\"test_im_2\"},{\"im_name\":\"test_im_3\"},{\"im_name\":\"test_im_4\"},{\"im_name\":\"test_im_5\"},{\"im_name\":\"test_im_6\"},{\"im_name\":\"test_im_7\"},{\"im_name\":\"test_im_8\"},{\"im_name\":\"test_im_9\"}]}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        SearchParams searchParams = new SearchParams("test_im");
        PagedSearchResult pagedResult = searchOperations.search(searchParams);
        assertEquals(new Integer(1), pagedResult.getPage());
        assertEquals(new Integer(10), pagedResult.getLimit());
        assertEquals(new Integer(20), pagedResult.getTotal());
        List<ImageResult> results = pagedResult.getResult();
        assertEquals(10, results.size());
        for (int i = 0; i < 10; i++) {
            ImageResult image = results.get(i);
            assertEquals("test_im_" + i, image.getImName());
        }
    }

    @Test
    public void testSearchResponseFacet() {
        String responseBody = "{\"status\":\"OK\",\"method\":\"search\",\"error\":[],\"page\":1,\"limit\":1,\"total\":20,\"result\":[{\"im_name\":\"test_im_0\"}],\"facets\":[{\"key\":\"brand\",\"items\":[{\"value\":\"brandA\",\"count\":5},{\"value\":\"brandB\",\"count\":6},{\"value\":\"brandC\",\"count\":9}]}]}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        SearchParams searchParams = new SearchParams("test_im")
                .setFacets(Lists.newArrayList("brand"));
        PagedSearchResult pagedResult = searchOperations.search(searchParams);
        assertEquals(new Integer(1), pagedResult.getPage());
        assertEquals(new Integer(1), pagedResult.getLimit());
        assertEquals(new Integer(20), pagedResult.getTotal());
        List<ImageResult> results = pagedResult.getResult();
        assertEquals(1, results.size());
        List<Facet> facetList = pagedResult.getFacets();
        assertEquals(1, facetList.size());
        Facet facet = facetList.get(0);
        assertEquals("brand", facet.getKey());
        List<FacetItem> facetItemList = facet.getFacetItems();
        assertEquals(3, facetItemList.size());
        FacetItem facetItem0 = facetItemList.get(0);
        assertEquals("brandA", facetItem0.getValue());
        assertEquals(new Integer(5), facetItem0.getCount());
        FacetItem facetItem1 = facetItemList.get(1);
        assertEquals("brandB", facetItem1.getValue());
        assertEquals(new Integer(6), facetItem1.getCount());
        FacetItem facetItem2 = facetItemList.get(2);
        assertEquals("brandC", facetItem2.getValue());
        assertEquals(new Integer(9), facetItem2.getCount());
    }

    @Test
    public void testSearchResponseFull() {
        String responseBody = "{\"status\":\"OK\",\"method\":\"search\",\"error\":[],\"page\":10,\"limit\":1,\"total\":20,\"qinfo\":{\"im_url\":\"http://www.example.com/test_im.jpeg\",\"price\":\"49.99\",\"title\":\"java sdk\"},\"result\":[{\"im_name\":\"test_im_0\",\"score\":0.43719249963760376,\"value_map\":{\"price\":\"67.500000\",\"title\":\"sdk test\"}}]}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        SearchParams searchParams = new SearchParams("test_im");
        PagedSearchResult pagedResult = searchOperations.search(searchParams);
        assertEquals(responseBody, pagedResult.getRawJson());
        assertEquals(new Integer(10), pagedResult.getPage());
        assertEquals(new Integer(1), pagedResult.getLimit());
        assertEquals(new Integer(20), pagedResult.getTotal());
        Map<String, String> qinfo = Maps.newHashMap();
        qinfo.put("im_url", "http://www.example.com/test_im.jpeg");
        qinfo.put("price", "49.99");
        qinfo.put("title", "java sdk");
        assertEquals(qinfo, pagedResult.getQueryInfo());
        List<ImageResult> results = pagedResult.getResult();
        ImageResult image = results.get(0);
        assertEquals("test_im_0", image.getImName());
        assertEquals(new Float(0.43719249963760376), image.getScore());
        Map<String, String> metadata = Maps.newHashMap();
        metadata.put("price", "67.500000");
        metadata.put("title", "sdk test");
        assertEquals(metadata, image.getMetadata());
    }

    @Test
    public void testSearchResponseError() {
        String responseBody = "{\"status\":\"fail\",\"method\":\"search\",\"error\":[\"Error message.\"],\"page\":1,\"limit\":10,\"total\":0}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        SearchParams searchParams = new SearchParams("test_im");
        PagedSearchResult pagedResult = searchOperations.search(searchParams);
        assertEquals("Error message.", pagedResult.getErrorMessage());
        assertEquals(null, pagedResult.getCause());
        assertEquals(responseBody, pagedResult.getRawResponseMessage());
    }

    @Test
    public void testSearchResponseUnknownError() {
        String responseBody = "{\"status\":\"fail\",\"method\":\"search\",\"page\":1,\"limit\":10,\"total\":0}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        SearchParams searchParams = new SearchParams("test_im");
        PagedSearchResult pagedResult = searchOperations.search(searchParams);
        assertEquals(ResponseMessages.INVALID_RESPONSE_FORMAT.getMessage(), pagedResult.getErrorMessage());
        assertEquals(responseBody, pagedResult.getRawResponseMessage());
    }

    @Test
    public void testSearchResponseErrorGetJson() {
        String responseBody = "{\"status\":\"fail\",\"method\":\"search\",\"error\":[\"Error message.\"],\"page\":1,\"limit\":10,\"total\":0}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        SearchParams searchParams = new SearchParams("test_im");
        PagedSearchResult pagedResult = searchOperations.search(searchParams);
        assertEquals("Error message.", pagedResult.getErrorMessage());
        assertEquals(responseBody, pagedResult.getRawResponseMessage());
    }

    @Test
    public void testSearchResponseMalformed0() {
        String responseBody = "{\"status\":\"OK\" \"method\":\"search\",\"error\":[],\"page\":1,\"limit\":10,\"total\":20,\"result\":[{\"im_name\":\"test_im_0\"},{\"im_name\":\"test_im_1\"},{\"im_name\":\"test_im_2\"},{\"im_name\":\"test_im_3\"},{\"im_name\":\"test_im_4\"},{\"im_name\":\"test_im_5\"},{\"im_name\":\"test_im_6\"},{\"im_name\":\"test_im_7\"},{\"im_name\":\"test_im_8\"},{\"im_name\":\"test_im_9\"}]}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        SearchParams searchParams = new SearchParams("test_im");
        PagedSearchResult pagedResult = searchOperations.search(searchParams);
        assertEquals(ResponseMessages.PARSE_RESPONSE_ERROR.getMessage(), pagedResult.getErrorMessage());
        assertEquals(responseBody, pagedResult.getRawResponseMessage());
    }

    @Test
    public void testSearchResponseMalformed1() {
        String responseBody = "{\"status\":\"OK\",\"method\":\"search\",\"error\":[],\"page\":1,\"limit\":10,\"total\":20,\"result\":{}}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        SearchParams searchParams = new SearchParams("test_im");
        PagedSearchResult pagedResult = searchOperations.search(searchParams);
        assertEquals(ResponseMessages.PARSE_RESPONSE_ERROR.getMessage(), pagedResult.getErrorMessage());
        assertEquals(responseBody, pagedResult.getRawResponseMessage());
    }

    @Test
    public void testSearchResponseMalformed2() {
        String responseBody = "{\"status\":\"OK\",\"error\":[],\"total\":20,\"result\":[]}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        SearchParams searchParams = new SearchParams("test_im");
        PagedSearchResult pagedResult = searchOperations.search(searchParams);
        assertEquals(ResponseMessages.INVALID_RESPONSE_FORMAT.getMessage(), pagedResult.getErrorMessage());
        assertEquals(responseBody, pagedResult.getRawResponseMessage());
    }

    @Test
    public void testSearchResponseMalformed3() throws Exception {
        String responseBody = "{\"status\":\"OK\",\"method\":\"search\",\"error\":[],\"page\":10,\"limit\":1,\"total\":20,\"qinfo\":[\"im_url\",\"price\",\"title\"],\"result\":[]}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        SearchParams searchParams = new SearchParams("test_im");
        PagedSearchResult pagedResult = searchOperations.search(searchParams);
        assertEquals(ResponseMessages.PARSE_RESPONSE_ERROR.getMessage(), pagedResult.getErrorMessage());
        assertEquals(responseBody, pagedResult.getRawResponseMessage());
    }

    @Test
    public void testSearchResponseMalformed4() {
        String responseBody = "{\"method\":\"search\",\"error\":[],\"page\":1,\"limit\":10,\"total\":0,\"result\":[]}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        SearchParams searchParams = new SearchParams("test_im");
        PagedSearchResult pagedResult = searchOperations.search(searchParams);
        assertEquals(ResponseMessages.INVALID_RESPONSE_FORMAT.getMessage(), pagedResult.getErrorMessage());
        assertEquals(responseBody, pagedResult.getRawResponseMessage());
    }

    @Test
    public void testColorSearchParams() {
        String responseBody = "{\"status\":\"OK\",\"method\":\"colorsearch\",\"error\":[],\"page\":1,\"limit\":10,\"total\":20,\"result\":[{\"im_name\":\"test_im_1\"}]}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        ColorSearchParams colorSearchParams = new ColorSearchParams("123ABC");
        assertEquals("123ABC", colorSearchParams.getColor());
        searchOperations.colorSearch(colorSearchParams);
        Multimap<String, String> expectedParams = HashMultimap.create();
        expectedParams.put("color", "123ABC");
        expectedParams.put("score", "false");
        verify(mockClient).get("/colorsearch", expectedParams);
    }

    @Test
    public void testColorSearchJsonFormatError() {
        String responseBody = "{\"status\":\"OK\" \"method\":\"colorsearch\",\"error\":[],\"page\":1,\"limit\":10,\"total\":20,\"result\":[{\"im_name\":\"test_im_1\"}]}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        ColorSearchParams colorSearchParams = new ColorSearchParams("123ABC");
        assertEquals("123ABC", colorSearchParams.getColor());
        PagedSearchResult searchResult = searchOperations.colorSearch(colorSearchParams);
        assertEquals(ResponseMessages.PARSE_RESPONSE_ERROR.getMessage(), searchResult.getErrorMessage());
        assertEquals(responseBody, searchResult.getRawResponseMessage());
    }

    @Test
    public void testColorSearchParamsInvalid() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Invalid color. " +
                "It should be a six hexadecimal number color code e.g. 123ACF.");
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        ColorSearchParams colorSearchParams = new ColorSearchParams("#123ABC");
    }

    @Test
    public void testUploadSearchParamsURL() {
        String responseBody = "{\"status\":\"OK\",\"method\":\"upload\",\"error\":[],\"page\":1,\"limit\":10,\"total\":20,\"result\":[{\"im_name\":\"test_im_1\"}]}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.post(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        UploadSearchParams uploadSearchParams = new UploadSearchParams("http://www.example.com/test_im.jpeg");
        assertEquals("http://www.example.com/test_im.jpeg", uploadSearchParams.getImageUrl());
        searchOperations.uploadSearch(uploadSearchParams);
        Multimap<String, String> expectedParams = HashMultimap.create();
        expectedParams.put("im_url", "http://www.example.com/test_im.jpeg");
        expectedParams.put("score", "false");
        verify(mockClient).post("/uploadsearch", expectedParams);
    }

    @Test
    public void testUploadSearchParamsURLWithDetection() {
        String responseBody = "{\"status\":\"OK\",\"method\":\"uploadsearch\",\"error\":[],\"page\":1,\"limit\":10,\"total\":20,\"product_types\":[{\"type\":\"top\",\"score\":1,\"box\":[84,223,425,639]},{\"type\":\"shoe\",\"score\":0.24432674050331116,\"box\":[522,77,865,337]},{\"type\":\"bag\",\"score\":0.2392924576997757,\"box\":[538,437,684,694]}],\"product_types_list\":[{\"type\":\"bag\"},{\"type\":\"bottom\"},{\"type\":\"dress\"},{\"type\":\"shoe\"},{\"type\":\"top\"},{\"type\":\"other\"}],\"result\":[],\"im_id\":\"abc.png\"}\n";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.post(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        UploadSearchParams uploadSearchParams = new UploadSearchParams("http://www.example.com/test_im.jpeg");
        uploadSearchParams.setDetection("dress");
        assertEquals("http://www.example.com/test_im.jpeg", uploadSearchParams.getImageUrl());
        PagedSearchResult uploadSearchResult = searchOperations.uploadSearch(uploadSearchParams);
        Multimap<String, String> expectedParams = HashMultimap.create();
        expectedParams.put("im_url", "http://www.example.com/test_im.jpeg");
        expectedParams.put("detection", "dress");
        expectedParams.put("score", "false");
        verify(mockClient).post("/uploadsearch", expectedParams);
        assertEquals(3, uploadSearchResult.getProductTypes().size());
        assertEquals(6, uploadSearchResult.getProductTypesList().size());
        for (ProductType productType : uploadSearchResult.getProductTypes()) {
            assertNotNull(productType.getType());
            assertNotNull(productType.getScore());
            assertNotNull(productType.getBox());
            assertEquals(4, productType.getBox().size());
        }
        for (ProductType productType : uploadSearchResult.getProductTypesList()) {
            assertNotNull(productType.getType());
            assertNull(productType.getScore());
            assertNull(productType.getBox());
        }

        assertEquals("abc.png", uploadSearchResult.getImId());
    }

    @Test
    public void testUploadSearchParamsNullFile() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("The image file must not be null");
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        File nullFile = null;
        UploadSearchParams uploadSearchParams = new UploadSearchParams(nullFile);
        searchOperations.uploadSearch(uploadSearchParams);
    }

    @Test
    public void testUploadSearchParamsNonFile() {
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        File nonFile = new File("nonFile");
        UploadSearchParams uploadSearchParams = new UploadSearchParams(nonFile);
        PagedSearchResult response = searchOperations.uploadSearch(uploadSearchParams);
        assertEquals(ResponseMessages.INVALID_IMAGE_OR_URL.getMessage(), response.getErrorMessage());
    }

    @Test
    public void testUploadSearchParamsNullStream() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("The image input stream must not be null");
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        InputStream inputStream = null;
        UploadSearchParams uploadSearchParams = new UploadSearchParams(inputStream);
        searchOperations.uploadSearch(uploadSearchParams);
    }

    @Test
    public void testDiscoverSearchParamsURL() {
        String responseBody = "{\"status\":\"OK\",\"method\":\"similarproducts\",\"error\":[],\"page\":1,\"limit\":10,\"total\":20,\"qinfo\":{\"im_url\":\"http://www.example.com/test_im.jpeg\",\"price\":\"49.99\",\"title\":\"java sdk\"},\"objects\":[{\"type\":\"top\",\"score\":1,\"box\":[84,223,425,639],\"result\":[{\"im_name\":\"test_im_0\",\"score\":0.43719249963760376,\"value_map\":{\"price\":\"67.500000\",\"title\":\"sdk test\"}}]},{\"type\":\"shoe\",\"score\":0.24432674050331116,\"box\":[522,77,865,337],\"result\":[{\"im_name\":\"test_im_0\",\"score\":0.43719249963760376,\"value_map\":{\"price\":\"67.500000\",\"title\":\"sdk test\"}}]},{\"type\":\"bag\",\"score\":0.2392924576997757,\"box\":[538,437,684,694],\"result\":[{\"im_name\":\"test_im_0\",\"score\":0.43719249963760376,\"value_map\":{\"price\":\"67.500000\",\"title\":\"sdk test\"}}]}],\"object_types_list\":[{\"type\":\"bag\"},{\"type\":\"bottom\"},{\"type\":\"dress\"},{\"type\":\"shoe\"},{\"type\":\"top\"},{\"type\":\"other\"}],\"im_id\":\"abc.png\"}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.post(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        UploadSearchParams uploadSearchParams = new UploadSearchParams("http://www.example.com/test_im.jpeg");
        assertEquals("http://www.example.com/test_im.jpeg", uploadSearchParams.getImageUrl());
        PagedSearchResult uploadSearchResult = searchOperations.discoverSearch(uploadSearchParams);
        Multimap<String, String> expectedParams = HashMultimap.create();
        expectedParams.put("im_url", "http://www.example.com/test_im.jpeg");
        expectedParams.put("score", "false");
        verify(mockClient).post("/discoversearch", expectedParams);
        assertEquals(3, uploadSearchResult.getObjects().size());
        assertEquals(6, uploadSearchResult.getObjectTypesList().size());
        for (ObjectSearchResult object : uploadSearchResult.getObjects()) {
            assertNotNull(object.getType());
            assertNotNull(object.getScore());
            assertNotNull(object.getBox());
            assertEquals(4, object.getBox().size());
        }
        for (ProductType productType : uploadSearchResult.getObjectTypesList()) {
            assertNotNull(productType.getType());
            assertNull(productType.getScore());
            assertNull(productType.getBox());
        }
        assertEquals(responseBody, uploadSearchResult.getRawJson());
        assertEquals(new Integer(1), uploadSearchResult.getPage());
        assertEquals(new Integer(10), uploadSearchResult.getLimit());
        assertEquals(new Integer(20), uploadSearchResult.getTotal());
        List<ObjectSearchResult> objects = uploadSearchResult.getObjects();
        assertEquals(3, objects.size());

        Map<String, String> qinfo = Maps.newHashMap();
        qinfo.put("im_url", "http://www.example.com/test_im.jpeg");
        qinfo.put("price", "49.99");
        qinfo.put("title", "java sdk");
        assertEquals(qinfo, uploadSearchResult.getQueryInfo());

        assertEquals("abc.png", uploadSearchResult.getImId());

        ImageResult image = objects.get(0).getResult().get(0);
        assertEquals("test_im_0", image.getImName());
        assertEquals(new Float(0.43719249963760376), image.getScore());
        Map<String, String> metadata = Maps.newHashMap();
        metadata.put("price", "67.500000");
        metadata.put("title", "sdk test");
        assertEquals(metadata, image.getMetadata());
    }

    @Test
    public void testDiscoverSearchParamsNullFile() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("The image file must not be null");
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        File nullFile = null;
        UploadSearchParams uploadSearchParams = new UploadSearchParams(nullFile);
        searchOperations.discoverSearch(uploadSearchParams);
    }

    @Test
    public void testDiscoverSearchParamsNonFile() {
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        File nonFile = new File("nonFile");
        UploadSearchParams uploadSearchParams = new UploadSearchParams(nonFile);
        PagedSearchResult response = searchOperations.discoverSearch(uploadSearchParams);
        assertEquals(ResponseMessages.INVALID_IMAGE_OR_URL.getMessage(), response.getErrorMessage());
    }

    @Test
    public void testSimilarProductsSearchParamsNullStream() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("The image input stream must not be null");
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        InputStream inputStream = null;
        UploadSearchParams uploadSearchParams = new UploadSearchParams(inputStream);
        PagedSearchResult response = searchOperations.similarProductsSearch(uploadSearchParams);
        assertEquals(ResponseMessages.INVALID_IMAGE_OR_URL.getMessage(), response.getErrorMessage());
    }

    // should not throw anything
    @Test
    public void testUploadSearchParamsImId() {
        String responseBody = "{\"status\":\"OK\",\"method\":\"uploadsearch\",\"error\":[],\"page\":1,\"limit\":10,\"total\":20,\"result\":[{\"im_name\":\"test_im_0\"},{\"im_name\":\"test_im_1\"},{\"im_name\":\"test_im_2\"},{\"im_name\":\"test_im_3\"},{\"im_name\":\"test_im_4\"},{\"im_name\":\"test_im_5\"},{\"im_name\":\"test_im_6\"},{\"im_name\":\"test_im_7\"},{\"im_name\":\"test_im_8\"},{\"im_name\":\"test_im_9\"}]}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.post(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);

        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        UploadSearchParams uploadSearchParams = new UploadSearchParams();
        uploadSearchParams.setImId("abc");
        PagedSearchResult sr = searchOperations.uploadSearch(uploadSearchParams);

        assertEquals(null, sr.getErrorMessage());

    }

    @Test
    public void testUploadSearchImFeature() {
        String responseBody = "{\"status\":\"OK\",\"method\":\"uploadsearch\",\"error\":[],\"page\":1,\"limit\":10,\"total\":10,\"result\":[{\"im_name\":\"test_im_0\"},{\"im_name\":\"test_im_1\"},{\"im_name\":\"test_im_2\"},{\"im_name\":\"test_im_3\"},{\"im_name\":\"test_im_4\"},{\"im_name\":\"test_im_5\"},{\"im_name\":\"test_im_6\"},{\"im_name\":\"test_im_7\"},{\"im_name\":\"test_im_8\"},{\"im_name\":\"test_im_9\"}]}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);

        // verify postImFeature is called when imfeature is set
        when(mockClient.postImFeature(anyString(), Matchers.<Multimap<String, String>>any(), anyString() , anyString() )).thenReturn(response);

        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        UploadSearchParams uploadSearchParams = new UploadSearchParams();
        uploadSearchParams.setImId("abc").setTransId("trans123").setImFeature("feature123");
        PagedSearchResult sr = searchOperations.uploadSearch(uploadSearchParams);

        assertEquals(null, sr.getErrorMessage());
    }

    @Test
    public void testSearchGroupedResponse() {
        String responseBody = "{\"status\":\"OK\",\"method\":\"uploadsearch\",\"error\":[],\"page\":1,\"group_by_key\":\"mpid\",\"group_limit\":2,\"total\":1000,\"product_types\":[{\"type\":\"shoe\",\"attributes\":{},\"score\":0.9999181032180786,\"box\":[41,256,577,489]}],\"product_types_list\":[{\"type\":\"bag\",\"attributes_list\":{\"gender\":[\"men\",\"women\"]}},{\"type\":\"bottom\",\"attributes_list\":{\"gender\":[\"men\",\"women\"]}},{\"type\":\"dress\",\"attributes_list\":{\"gender\":[\"women\"]}},{\"type\":\"eyewear\",\"attributes_list\":{\"subcategory\":[\"sunglasses\",\"eyeglasses\"]}},{\"type\":\"jewelry\",\"attributes_list\":{}},{\"type\":\"outerwear\",\"attributes_list\":{}},{\"type\":\"shoe\",\"attributes_list\":{\"gender\":[\"men\",\"women\"]}},{\"type\":\"skirt\",\"attributes_list\":{\"gender\":[\"women\"]}},{\"type\":\"top\",\"attributes_list\":{\"gender\":[\"men\",\"women\"],\"subcategory\":[\"sweater\",\"top&tshirt\",\"shirt\"]}},{\"type\":\"watch\",\"attributes_list\":{}},{\"type\":\"other\",\"attributes_list\":{}}],\"group_results\":[{\"group_by_value\":\"321642abb52c014a0861c3264ebd3a04\",\"result\":[{\"im_name\":\"a46453f90a3a14bb574e43c7c51cb828\"}]},{\"group_by_value\":\"053028d324e736d02e1c9aa97d53dcf9\",\"result\":[{\"im_name\":\"053028d324e736d02e1c9aa97d53dcf9\"}]}],\"im_id\":\"2017062864e69285a9eb6940bc5089b7d39db6a52561269e.jpg\",\"reqid\":\"647331357030156285\"}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        SearchParams searchParams = new SearchParams("test_im");

        PagedSearchResult pagedResult = searchOperations.search(searchParams);
        assertEquals(new Integer(1), pagedResult.getPage());
        assertEquals(new Integer(2), pagedResult.getGroupLimit());
        assertEquals(new Integer(1000), pagedResult.getTotal());
        List<ImageResult> results = pagedResult.getResult();

        assertEquals(0, results.size());

        List<GroupSearchResult> groups = pagedResult.getGroupSearchResults();
        assertEquals(2, groups.size());

        GroupSearchResult firstItem = groups.get(0);
        assertEquals("321642abb52c014a0861c3264ebd3a04", firstItem.getGroupByValue());
        assertEquals(1, firstItem.getResult().size());
        assertEquals("a46453f90a3a14bb574e43c7c51cb828", firstItem.getResult().get(0).getImName());
        assertEquals("2017062864e69285a9eb6940bc5089b7d39db6a52561269e.jpg", pagedResult.getImId());
        assertEquals(1, pagedResult.getProductTypes().size());
        assertEquals("shoe", pagedResult.getProductTypes().get(0).getType());
        assertEquals("mpid", pagedResult.getGroupByKey());

    }

    @Test
    public void testExtractFeaturesImId() {
        String responseBody = "{\n" +
                "    \"status\": \"OK\",\n" +
                "    \"method\": \"extractfeature\",\n" +
                "    \"error\": [],\n" +
                "    \"result\": [\n" +
                "    \t\"aaab\"\n" +
                "    ],\n" +
                "    \"reqid\": \"651567729928636598\",\n" +
                "    \"im_id\": \"20170710b9b9146bda166aa38aebfb43548666b3011fe38f.jpg\"\n" +
                "}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        Map<String, String> responseHeaders = Maps.newHashMap();
        responseHeaders.put("X-Log-ID", "651567729928636598");
        when(response.getHeaders()).thenReturn(responseHeaders);

        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.post(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);

        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        UploadSearchParams uploadSearchParams = new UploadSearchParams();
        uploadSearchParams.setImId("abc");
        FeatureResponseResult fr = searchOperations.extractFeature(uploadSearchParams);

        assertEquals(null, fr.getErrorMessage());
        assertEquals(fr.getImId() , "20170710b9b9146bda166aa38aebfb43548666b3011fe38f.jpg");
        assertEquals(fr.getReqId(), "651567729928636598");

        List<String> result = fr.getResult();
        assertEquals(result.size(), 1);
        assertEquals(result.get(0), "aaab");
    }

    @Test
    public void testExtractFeatureProductTypes(){
        String responseBody = "{\n" +
                "    \"status\": \"OK\",\n" +
                "    \"method\": \"extractfeature\",\n" +
                "    \"error\": [],\n" +
                "    \"product_types\": [\n" +
                "        {\n" +
                "            \"type\": \"pant\",\n" +
                "            \"attributes\": {},\n" +
                "            \"score\": 0,\n" +
                "            \"box\": [\n" +
                "                49,\n" +
                "                0,\n" +
                "                1095,\n" +
                "                1187\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"shoe\",\n" +
                "            \"attributes\": {},\n" +
                "            \"score\": 0,\n" +
                "            \"box\": [\n" +
                "                563,\n" +
                "                53,\n" +
                "                842,\n" +
                "                955\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"other\",\n" +
                "            \"attributes\": {},\n" +
                "            \"score\": 0,\n" +
                "            \"box\": [\n" +
                "                805,\n" +
                "                31,\n" +
                "                1198,\n" +
                "                1125\n" +
                "            ]\n" +
                "        }\n" +
                "    ],\n" +
                "    \"product_types_list\": [\n" +
                "        {\n" +
                "            \"type\": \"bag\",\n" +
                "            \"attributes_list\": {}\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"bottom\",\n" +
                "            \"attributes_list\": {}\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"dress\",\n" +
                "            \"attributes_list\": {}\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"eyewear\",\n" +
                "            \"attributes_list\": {}\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"jewelry\",\n" +
                "            \"attributes_list\": {}\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"outerwear\",\n" +
                "            \"attributes_list\": {}\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"shoe\",\n" +
                "            \"attributes_list\": {}\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"skirt\",\n" +
                "            \"attributes_list\": {}\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"top\",\n" +
                "            \"attributes_list\": {}\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"watch\",\n" +
                "            \"attributes_list\": {}\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"other\",\n" +
                "            \"attributes_list\": {}\n" +
                "        }\n" +
                "    ],\n" +
                "    \"result\": [\n" +
                "        \"EIAAAAAAA\"\n" +
                "    ],\n" +
                "    \"reqid\": \"685767049926007407\",\n" +
                "    \"im_id\": \"20171012b9b9146bda166aa38aebfb43548666b3011fe38f.jpg\"\n" +
                "}";

        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        Map<String, String> responseHeaders = Maps.newHashMap();
        responseHeaders.put("X-Log-ID", "685767049926007407");
        when(response.getHeaders()).thenReturn(responseHeaders);

        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.post(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);

        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        UploadSearchParams uploadSearchParams = new UploadSearchParams();
        uploadSearchParams.setImId("abc");
        FeatureResponseResult fr = searchOperations.extractFeature(uploadSearchParams);

        assertEquals(null, fr.getErrorMessage());
        assertEquals(fr.getImId() , "20171012b9b9146bda166aa38aebfb43548666b3011fe38f.jpg");
        assertEquals(fr.getReqId(), "685767049926007407");

        List<String> result = fr.getResult();
        assertEquals(result.size(), 1);
        assertEquals(result.get(0), "EIAAAAAAA");

        List<ProductType> productTypes = fr.getProductTypes();
        assertEquals(productTypes.size(), 3);
        assertEquals(productTypes.get(0).getType(), "pant");
        assertEquals(Joiner.on(",").join(productTypes.get(0).getBox())  , "49,0,1095,1187"  );

        assertEquals(productTypes.get(1).getType(), "shoe");
        assertEquals(productTypes.get(2).getType(), "other");

        assertEquals(fr.getProductTypesList().size(), 11);

    }

    @Test
    public void testExtractFeatureNullFile() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("The image file must not be null");
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        File nullFile = null;
        UploadSearchParams uploadSearchParams = new UploadSearchParams(nullFile);
        searchOperations.extractFeature(uploadSearchParams);
    }

    @Test
    public void testExtractFeatureNonFile() {
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        File nonFile = new File("nonFile");
        UploadSearchParams uploadSearchParams = new UploadSearchParams(nonFile);
        FeatureResponseResult response = searchOperations.extractFeature(uploadSearchParams);
        assertEquals(ResponseMessages.INVALID_IMAGE_OR_URL.getMessage(), response.getErrorMessage());
    }

    @Test
    public void testExtractFeatureNullStream() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("The image input stream must not be null");
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        InputStream inputStream = null;
        UploadSearchParams uploadSearchParams = new UploadSearchParams(inputStream);
        searchOperations.extractFeature(uploadSearchParams);
    }

    @Test
    public void testDiscoverSearchMerged() {
        String responseBody = "{\n" +
                "    \"status\":\"OK\",\n" +
                "    \"method\":\"discoversearch\",\n" +
                "    \"error\":[\n" +
                "        \n" +
                "    ],\n" +
                "    \"result_limit\":1,\n" +
                "    \"detection_limit\":10,\n" +
                "    \"page\":1,\n" +
                "    \"objects\":[\n" +
                "        {\n" +
                "            \"type\":\"A\",\n" +
                "            \"attributes\":{\n" +
                "                \n" +
                "            },\n" +
                "            \"score\":1,\n" +
                "            \"box\":[\n" +
                "                235,\n" +
                "                68,\n" +
                "                766,\n" +
                "                205\n" +
                "            ],\n" +
                "            \"total\":100,\n" +
                "            \"result\":[\n" +
                "                \n" +
                "            ],\n" +
                "            \"facets\":[\n" +
                "                \n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\":\"B\",\n" +
                "            \"attributes\":{\n" +
                "                \n" +
                "            },\n" +
                "            \"score\":0.88,\n" +
                "            \"box\":[\n" +
                "                0,\n" +
                "                0,\n" +
                "                778,\n" +
                "                215\n" +
                "            ],\n" +
                "            \"total\":100,\n" +
                "            \"result\":[\n" +
                "                \n" +
                "            ],\n" +
                "            \"facets\":[\n" +
                "                \n" +
                "            ]\n" +
                "        }\n" +
                "    ],\n" +
                "    \"object_types_list\":[\n" +
                "        {\n" +
                "            \"type\":\"ABC\",\n" +
                "            \"attributes_list\":{\n" +
                "                \n" +
                "            }\n" +
                "        }\n" +
                "    ],\n" +
                "    \"im_id\":\"201711167965720e6c058f0cb57eb3316908471324503b7a.jpg\",\n" +
                "    \"reqid\":\"698371275147490045\",\n" +
                "    \"result\":[\n" +
                "        {\n" +
                "            \"im_name\":\"test_im_0\",\n" +
                "            \"score\":0.43719249963760376,\n" +
                "            \"value_map\":{\n" +
                "                \"im_url\":\"http://test.JPG\"\n" +
                "            }\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.post(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        UploadSearchParams uploadSearchParams = new UploadSearchParams("http://www.example.com/test_im.jpeg");
        assertEquals("http://www.example.com/test_im.jpeg", uploadSearchParams.getImageUrl());

        PagedSearchResult uploadSearchResult = searchOperations.discoverSearch(uploadSearchParams);
        Multimap<String, String> expectedParams = HashMultimap.create();
        expectedParams.put("im_url", "http://www.example.com/test_im.jpeg");
        expectedParams.put("score", "false");

        verify(mockClient).post("/discoversearch", expectedParams);

        assertEquals(new Integer(1), uploadSearchResult.getPage());
        assertEquals(1, uploadSearchResult.getResult().size());

        ImageResult image = uploadSearchResult.getResult().get(0) ;
        assertEquals("test_im_0", image.getImName());
        assertEquals(new Float(0.43719249963760376), image.getScore());
        Map<String, String> metadata = Maps.newHashMap();
        metadata.put("im_url", "http://test.JPG");
        assertEquals(metadata, image.getMetadata());

        assertEquals(1, uploadSearchResult.getObjectTypesList().size());
        assertEquals(uploadSearchResult.getObjectTypesList().get(0).getType() , "ABC");
        assertEquals(0, uploadSearchResult.getObjectTypesList().get(0).getAttributesList().size() );

        assertEquals(2, uploadSearchResult.getObjects().size());

        ObjectSearchResult object = uploadSearchResult.getObjects().get(0);
        assertEquals(object.getType(), "A");
        assertEquals(object.getScore().toString(), "1.0");
        assertEquals(object.getTotal(), 100);
        assertEquals(0, object.getResult().size());
        assertEquals("235,68,766,205", Joiner.on(",").join(object.getBox()) );

        ObjectSearchResult object2 = uploadSearchResult.getObjects().get(1);
        assertEquals(object2.getType(), "B");
        assertEquals(object2.getScore().toString(), "0.88");
        assertEquals(object2.getTotal(), 100);
        assertEquals(0, object2.getResult().size());
        assertEquals("0,0,778,215", Joiner.on(",").join(object2.getBox()) );


    }

    @Test
    public void testUploadSearchParamsURLUploadSearch() {
        String responseBody = "{\"status\":\"OK\",\"method\":\"upload\",\"error\":[],\"page\":1,\"limit\":10,\"total\":20,\"result\":[{\"im_name\":\"test_im_1\", \"score\":0.43719249963760376,\"value_map\":{\"price\":\"67.500000\",\"title\":\"sdk test\"} }, {\"im_name\":\"test_im_2\", \"score\":0.56,\"value_map\":{\"price\":\"88.500000\",\"title\":\"sdk test 2\"} } ]}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.post(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        UploadSearchParams uploadSearchParams = new UploadSearchParams("http://www.example.com/test_im.jpeg");
        uploadSearchParams.setSortBy("price:asc");
        assertEquals("http://www.example.com/test_im.jpeg", uploadSearchParams.getImageUrl());

        PagedSearchResult result = searchOperations.uploadSearch(uploadSearchParams);

        Multimap<String, String> expectedParams = HashMultimap.create();
        expectedParams.put("im_url", "http://www.example.com/test_im.jpeg");
        expectedParams.put("score", "false");
        expectedParams.put("sort_by", "price:asc");

        verify(mockClient).post("/uploadsearch", expectedParams);

        assertEquals(2, result.getResult().size());
        ImageResult r1 = result.getResult().get(0);
        assertEquals("test_im_1", r1.getImName());
        assertEquals(0.43719249963760376, r1.getScore(), 0.0001);
        assertEquals("67.500000", r1.getMetadata().get("price"));

        ImageResult r2 = result.getResult().get(1);
        assertEquals("test_im_2", r2.getImName());
        assertEquals(0.56, r2.getScore(), 0.0001);
        assertEquals("88.500000", r2.getMetadata().get("price"));
    }


    @Test
    public void testSearchGroupedResponseSortBy() {
        String responseBody = "{\"status\":\"OK\",\"method\":\"search\",\"error\":[],\"page\":1,\"group_by_key\":\"mpid\",\"group_limit\":2,\"total\":1000,\"product_types\":[{\"type\":\"shoe\",\"attributes\":{},\"score\":0.9999181032180786,\"box\":[41,256,577,489]}],\"product_types_list\":[{\"type\":\"bag\",\"attributes_list\":{\"gender\":[\"men\",\"women\"]}},{\"type\":\"bottom\",\"attributes_list\":{\"gender\":[\"men\",\"women\"]}},{\"type\":\"dress\",\"attributes_list\":{\"gender\":[\"women\"]}},{\"type\":\"eyewear\",\"attributes_list\":{\"subcategory\":[\"sunglasses\",\"eyeglasses\"]}},{\"type\":\"jewelry\",\"attributes_list\":{}},{\"type\":\"outerwear\",\"attributes_list\":{}},{\"type\":\"shoe\",\"attributes_list\":{\"gender\":[\"men\",\"women\"]}},{\"type\":\"skirt\",\"attributes_list\":{\"gender\":[\"women\"]}},{\"type\":\"top\",\"attributes_list\":{\"gender\":[\"men\",\"women\"],\"subcategory\":[\"sweater\",\"top&tshirt\",\"shirt\"]}},{\"type\":\"watch\",\"attributes_list\":{}},{\"type\":\"other\",\"attributes_list\":{}}],\"group_results\":[{\"group_by_value\":\"321642abb52c014a0861c3264ebd3a04\",\"result\":[{\"im_name\":\"a46453f90a3a14bb574e43c7c51cb828\"}]},{\"group_by_value\":\"053028d324e736d02e1c9aa97d53dcf9\",\"result\":[{\"im_name\":\"053028d324e736d02e1c9aa97d53dcf9\"}]}],\"im_id\":\"2017062864e69285a9eb6940bc5089b7d39db6a52561269e.jpg\",\"reqid\":\"647331357030156285\"}";
        ViSearchHttpResponse response = mock(ViSearchHttpResponse.class);
        when(response.getBody()).thenReturn(responseBody);
        when(mockClient.get(anyString(), Matchers.<Multimap<String, String>>any())).thenReturn(response);
        SearchOperations searchOperations = new SearchOperationsImpl(mockClient, objectMapper);
        SearchParams searchParams = new SearchParams("test_im");
        searchParams.setSortGroupBy("price:asc");
        searchParams.setSortGroupStrategy("first");

        Multimap<String, String> expectedParams = HashMultimap.create();
        expectedParams.put("im_name", "test_im");
        expectedParams.put("score", "false");
        expectedParams.put("sort_group_by", "price:asc");
        expectedParams.put("sort_group_strategy", "first");

        PagedSearchResult pagedResult = searchOperations.search(searchParams);
        verify(mockClient).get("/search", expectedParams);

        assertEquals(new Integer(1), pagedResult.getPage());
        assertEquals(new Integer(2), pagedResult.getGroupLimit());
        assertEquals(new Integer(1000), pagedResult.getTotal());
        List<ImageResult> results = pagedResult.getResult();

        assertEquals(0, results.size());

        List<GroupSearchResult> groups = pagedResult.getGroupSearchResults();
        assertEquals(2, groups.size());

        GroupSearchResult firstItem = groups.get(0);
        assertEquals("321642abb52c014a0861c3264ebd3a04", firstItem.getGroupByValue());
        assertEquals(1, firstItem.getResult().size());
        assertEquals("a46453f90a3a14bb574e43c7c51cb828", firstItem.getResult().get(0).getImName());
        assertEquals("2017062864e69285a9eb6940bc5089b7d39db6a52561269e.jpg", pagedResult.getImId());
        assertEquals(1, pagedResult.getProductTypes().size());
        assertEquals("shoe", pagedResult.getProductTypes().get(0).getType());
        assertEquals("mpid", pagedResult.getGroupByKey());

    }
}
