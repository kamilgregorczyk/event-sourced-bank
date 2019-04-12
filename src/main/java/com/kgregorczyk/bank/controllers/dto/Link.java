package com.kgregorczyk.bank.controllers.dto;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Getter;
import spark.route.HttpMethod;

import java.util.List;
import java.util.UUID;

/** Represents a link for HATEOAS responses. */
@Getter
@Builder
public class Link {

  private static final ImmutableList<Link> LINKS_FOR_ACCOUNTS =
      ImmutableList.of(
          Link.builder().rel("self").href("/api/account").method(HttpMethod.get).build(),
          Link.builder().rel("self").href("/api/account").method(HttpMethod.post).build(),
          Link.builder()
              .rel("self")
              .href("/api/account/transferMoney")
              .method(HttpMethod.post)
              .build());

  private String rel;
  private String href;
  private HttpMethod method;

  public static List<Link> getLinksForAccounts() {
    return LINKS_FOR_ACCOUNTS;
  }

  public static List<Link> getLinksForAccount(UUID aggreagateUUID) {
    return getLinksForAccount(aggreagateUUID.toString());
  }

  public static List<Link> getLinksForAccount(String aggregateUUID) {
    return ImmutableList.of(
        Link.builder()
            .rel("self")
            .href("/api/account/" + aggregateUUID)
            .method(HttpMethod.get)
            .build(),
        Link.builder()
            .rel("self")
            .href("/api/account/" + aggregateUUID + "/changeFullName")
            .method(HttpMethod.put)
            .build());
  }
}
