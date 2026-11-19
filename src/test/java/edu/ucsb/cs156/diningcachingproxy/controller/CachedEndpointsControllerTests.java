package edu.ucsb.cs156.diningcachingproxy.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.diningcachingproxy.ControllerTestCase;
import edu.ucsb.cs156.diningcachingproxy.collections.CachedResponse;
import edu.ucsb.cs156.diningcachingproxy.collections.CachedResponseRepository;
import java.util.Arrays;
import java.util.List;
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

  /*
  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_out_users_cannot_delete() throws Exception {
    mockMvc
        .perform(delete("/api/admin/endpoints/delete").param("id", "/dining/commons/v1/"))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void logged_in_admins_can_delete() throws Exception {
    CachedResponse commons =
        CachedResponse.builder()
            .requestPath("/dining/commons/v1/")
            .responseStatus(200)
            .responseBody("[]")
            .hitCount(5)
            .build();
  // TODO: Update these tests
    when(hostManagerRepository.findByEmail("hm@ucsb.edu")).thenReturn(Optional.of(hostManager));

    MvcResult response =
        mockMvc
            .perform(
                delete("/api/admin/hostmanagers/delete").param("email", "hm@ucsb.edu").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(hostManagerRepository, times(1)).findByEmail("hm@ucsb.edu");
    verify(hostManagerRepository, times(1)).delete(hostManager);
    Map<String, Object> json = responseToJson(response);
    assertEquals("HostManager with id hm@ucsb.edu deleted", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_tries_to_delete_a_hostmanager_not_found() throws Exception {
    String email = "nonexistent@ucsb.edu";
    when(hostManagerRepository.findByEmail(email)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(delete("/api/admin/hostmanagers/delete").param("email", email).with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(hostManagerRepository, times(1)).findByEmail(email);
    verify(hostManagerRepository, times(0)).delete(any());
    Map<String, Object> json = responseToJson(response);
    assertEquals("HostManager with id nonexistent@ucsb.edu not found", json.get("message"));
  }
    */
}
