package edu.ucsb.cs156.diningcachingproxy.controller;

import edu.ucsb.cs156.diningcachingproxy.collections.CachedResponse;
import edu.ucsb.cs156.diningcachingproxy.collections.CachedResponseRepository;
import edu.ucsb.cs156.diningcachingproxy.errors.EntityNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets an admin see every API endpoint the proxy has cached so far, with a hit count for each — the
 * observation-only groundwork for per-endpoint TTL policy (see issue #2).
 */
@Tag(name = "Admin - Cached Endpoints")
@RequestMapping("/api/admin/endpoints")
@RestController
public class CachedEndpointsController extends ApiController {

  @Autowired private CachedResponseRepository cachedResponseRepository;

  public record CachedEndpointDTO(
      String requestPath, int responseStatus, String responseBody, long hitCount) {
    public CachedEndpointDTO(CachedResponse cachedResponse) {
      this(
          cachedResponse.getRequestPath(),
          cachedResponse.getResponseStatus(),
          cachedResponse.getResponseBody(),
          cachedResponse.getHitCount());
    }
  }

  @Operation(summary = "List all API endpoints the proxy has cached, with hit counts")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @GetMapping("")
  public List<CachedEndpointDTO> allEndpoints() {
    return cachedResponseRepository.findAll().stream().map(CachedEndpointDTO::new).toList();
  }

  @Operation(summary = "Purge a single cached entry")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @DeleteMapping("/delete")
  public Object deleteEndpointEntry(@Parameter(name = "id") @RequestParam String id) {
    CachedResponse cachedResponse =
        cachedResponseRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(CachedResponse.class, id));
    cachedResponseRepository.deleteById(id);
    return genericMessage("Cached entry with id %s deleted".formatted(id));
  }
}
