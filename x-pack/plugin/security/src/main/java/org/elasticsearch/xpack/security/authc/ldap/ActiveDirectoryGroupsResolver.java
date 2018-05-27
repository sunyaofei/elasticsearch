/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.security.authc.ldap;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.xpack.core.security.authc.ldap.support.LdapSearchScope;
import org.elasticsearch.xpack.security.authc.ldap.support.LdapSession.GroupsResolver;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.elasticsearch.xpack.core.security.authc.ldap.ActiveDirectorySessionFactorySettings.AD_DOMAIN_NAME_SETTING;
import static org.elasticsearch.xpack.security.authc.ldap.ActiveDirectorySessionFactory.buildDnFromDomain;
import static org.elasticsearch.xpack.security.authc.ldap.support.LdapUtils.OBJECT_CLASS_PRESENCE_FILTER;
import static org.elasticsearch.xpack.security.authc.ldap.support.LdapUtils.search;
import static org.elasticsearch.xpack.security.authc.ldap.support.LdapUtils.searchForEntry;
import static org.elasticsearch.xpack.core.security.authc.ldap.support.SessionFactorySettings.IGNORE_REFERRAL_ERRORS_SETTING;
import static org.elasticsearch.xpack.security.authc.ldap.ActiveDirectorySIDUtil.convertToString;

class ActiveDirectoryGroupsResolver implements GroupsResolver {

    private static final String TOKEN_GROUPS = "tokenGroups";
    private final String baseDn;
    private final LdapSearchScope scope;
    private final boolean ignoreReferralErrors;

    ActiveDirectoryGroupsResolver(Settings settings) {
        this.baseDn = settings.get("group_search.base_dn", buildDnFromDomain(settings.get(AD_DOMAIN_NAME_SETTING)));
        this.scope = LdapSearchScope.resolve(settings.get("group_search.scope"), LdapSearchScope.SUB_TREE);
        this.ignoreReferralErrors = IGNORE_REFERRAL_ERRORS_SETTING.get(settings);
    }

    @Override
    public void resolve(LDAPInterface connection, String userDn, TimeValue timeout, Logger logger, Collection<Attribute> attributes,
                        ActionListener<List<String>> listener) {
        buildGroupQuery(connection, userDn, timeout,
                ignoreReferralErrors, ActionListener.wrap((filter) -> {
                    if (filter == null) {
                        listener.onResponse(Collections.emptyList());
                    } else {
                        logger.debug("group SID to DN [{}] search filter: [{}]", userDn, filter);
                        search(connection, baseDn, scope.scope(), filter,
                                Math.toIntExact(timeout.seconds()), ignoreReferralErrors,
                                ActionListener.wrap((results) -> {
                                            List<String> groups = results.stream()
                                                    .map(SearchResultEntry::getDN)
                                                    .collect(Collectors.toList());
                                            listener.onResponse(Collections.unmodifiableList(groups));
                                        },
                                        listener::onFailure),
                                SearchRequest.NO_ATTRIBUTES);
                    }
                }, listener::onFailure));
    }

    @Override
    public String[] attributes() {
        // we have to return null since the tokenGroups attribute is computed and can only be retrieved using a BASE level search
        return null;
    }

    static void buildGroupQuery(LDAPInterface connection, String userDn, TimeValue timeout,
                                boolean ignoreReferralErrors, ActionListener<Filter> listener) {
        searchForEntry(connection, userDn, SearchScope.BASE, OBJECT_CLASS_PRESENCE_FILTER,
                Math.toIntExact(timeout.seconds()), ignoreReferralErrors,
                ActionListener.wrap((entry) -> {
                    if (entry == null || entry.hasAttribute(TOKEN_GROUPS) == false) {
                        listener.onResponse(null);
                    } else {
                        final byte[][] tokenGroupSIDBytes = entry.getAttributeValueByteArrays(TOKEN_GROUPS);
                        List<Filter> orFilters = Arrays.stream(tokenGroupSIDBytes)
                                .map((sidBytes) -> Filter.createEqualityFilter("objectSid", convertToString(sidBytes)))
                                .collect(Collectors.toList());
                        listener.onResponse(Filter.createORFilter(orFilters));
                    }
                }, listener::onFailure),
                TOKEN_GROUPS);
    }

}
