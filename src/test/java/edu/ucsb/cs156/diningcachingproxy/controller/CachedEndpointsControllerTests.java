package edu.ucsb.cs156.diningcachingproxy.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.diningcachingproxy.ControllerTestCase;
import edu.ucsb.cs156.diningcachingproxy.collections.CachedResponse;
import edu.ucsb.cs156.diningcachingproxy.collections.CachedResponseRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = CachedEndpointsController.class)
public class CachedEndpointsControllerTests extends ControllerTestCase {

  @MockitoBean CachedResponseRepository cachedResponseRepository;

  @Test
  public void logged_out_users_cannot_get_endpoints() throws Exception {
    mockMvc.perform(get("/api/admin/endpoints")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_get_endpoints() throws Exception {
    mockMvc.perform(get("/api/admin/endpoints")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_list_cached_endpoints_with_response_header_and_body_and_hit_counts()
      throws Exception {
    CachedResponse commons =
        CachedResponse.builder()
            .requestPath("/dining/commons/v1/")
            .responseStatus(200)
            .responseBody("[]")
            .hitCount(5)
            .build();
    CachedResponse menu =
        CachedResponse.builder()
            .requestPath("/dining/menu/v1/2024-08-16/carrillo")
            .responseStatus(200)
            .responseBody("{}")
            .hitCount(1)
            .build();
    when(cachedResponseRepository.findAll()).thenReturn(Arrays.asList(commons, menu));

    MvcResult response =
        mockMvc.perform(get("/api/admin/endpoints")).andExpect(status().isOk()).andReturn();

    List<CachedEndpointsController.CachedEndpointDTO> expected =
        List.of(
            new CachedEndpointsController.CachedEndpointDTO("/dining/commons/v1/", 200, "[]", 5),
            new CachedEndpointsController.CachedEndpointDTO(
                "/dining/menu/v1/2024-08-16/carrillo", 200, "{}", 1));
    String expectedJson = mapper.writeValueAsString(expected);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void list_cached_endpoints_truncates_long_response_bodies() throws Exception {
    String commonsResponseBody = "[\"Carillo\", \"DLG\", \"Ortega\", \"Portola\"]";
    String menuResponseBody =
        "[{\"name\":\"Pepperoni Pizza\",\"station\":\"Pizza\"},{\"name\":\"Cheese Pizza\",\"station\":\"Pizza\"},{\"name\":\"Vegetable Pizza on Wheat\",\"station\":\"Pizza\"}]";
    String truncatedMenuResponseBody =
        "[{\"name\":\"Pepperoni Pizza\",\"station\":\"Pizza\"},{\"name\":\"Cheese Pizza\",\"station\":\"Pizza\"},{\"name\":\"Vegetable Pizza o...[truncated]";

    CachedResponse commons =
        CachedResponse.builder()
            .requestPath("/dining/commons/v1/")
            .responseStatus(200)
            .responseBody(commonsResponseBody)
            .hitCount(5)
            .build();
    CachedResponse menu =
        CachedResponse.builder()
            .requestPath("/dining/menu/v1/2024-08-16/carrillo")
            .responseStatus(200)
            .responseBody(menuResponseBody)
            .hitCount(1)
            .build();
    when(cachedResponseRepository.findAll()).thenReturn(Arrays.asList(commons, menu));

    MvcResult response =
        mockMvc.perform(get("/api/admin/endpoints")).andExpect(status().isOk()).andReturn();

    List<CachedEndpointsController.CachedEndpointDTO> expected =
        List.of(
            new CachedEndpointsController.CachedEndpointDTO(
                "/dining/commons/v1/", 200, commonsResponseBody, 5),
            new CachedEndpointsController.CachedEndpointDTO(
                "/dining/menu/v1/2024-08-16/carrillo", 200, truncatedMenuResponseBody, 1));
    String expectedJson = mapper.writeValueAsString(expected);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @Test
  public void logged_out_users_cannot_delete_endpoints() throws Exception {
    mockMvc
        .perform(delete("/api/admin/endpoints/delete").param("requestPath", "/dining/commons/v1/"))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_delete_endpoints() throws Exception {
    mockMvc
        .perform(delete("/api/admin/endpoints/delete").param("requestPath", "/dining/commons/v1/"))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void logged_in_admins_can_delete_cached_endpoints() throws Exception {
    CachedResponse commons =
        CachedResponse.builder()
            .requestPath("/dining/commons/v1/")
            .responseStatus(200)
            .responseBody("[]")
            .hitCount(5)
            .build();
    when(cachedResponseRepository.findByRequestPath("/dining/commons/v1/"))
        .thenReturn(Optional.of(commons));

    MvcResult response =
        mockMvc
            .perform(
                delete("/api/admin/endpoints/delete")
                    .param("requestPath", "/dining/commons/v1/")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(cachedResponseRepository, times(1)).findByRequestPath("/dining/commons/v1/");
    verify(cachedResponseRepository, times(1)).delete(commons);
    Map<String, Object> json = responseToJson(response);
    assertEquals("Cached entry for request path /dining/commons/v1/ deleted", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_tries_to_delete_a_cached_entry_not_found() throws Exception {
    String requestPath = "/dining/nonexistent/v1";
    when(cachedResponseRepository.findByRequestPath(requestPath)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                delete("/api/admin/endpoints/delete")
                    .param("requestPath", requestPath)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(cachedResponseRepository, times(1)).findByRequestPath(requestPath);
    verify(cachedResponseRepository, times(0)).delete(any());
    Map<String, Object> json = responseToJson(response);
    assertEquals("CachedResponse with id /dining/nonexistent/v1 not found", json.get("message"));
  }
}
