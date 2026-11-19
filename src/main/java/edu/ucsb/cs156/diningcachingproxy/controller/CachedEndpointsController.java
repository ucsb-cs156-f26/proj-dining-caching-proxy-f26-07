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
          maybeTruncateResponseBody(cachedResponse.getResponseBody()),
          cachedResponse.getHitCount());
    }

    // Returns the truncated response body text if it exceeds the set maxResponseBodyLength
    // Otherwise returns the original response body
    private static String maybeTruncateResponseBody(String responseBody) {
      String truncateFlag = "...[truncated]";
      int maxResponseBodyLength = 128;
      if (responseBody.length() > maxResponseBodyLength) {
        responseBody =
            responseBody.substring(0, maxResponseBodyLength - truncateFlag.length()) + truncateFlag;
      }
      return responseBody;
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
  public Object deleteEndpointEntry(
      @Parameter(name = "requestPath") @RequestParam String requestPath) {
    CachedResponse cachedResponse =
        cachedResponseRepository
            .findByRequestPath(requestPath)
            .orElseThrow(() -> new EntityNotFoundException(CachedResponse.class, requestPath));
    cachedResponseRepository.delete(cachedResponse);
    return genericMessage("Cached entry for request path %s deleted".formatted(requestPath));
  }
}
