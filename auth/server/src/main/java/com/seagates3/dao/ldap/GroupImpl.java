/*
 * COPYRIGHT 2016 SEAGATE LLC
 *
 * THIS DRAWING/DOCUMENT, ITS SPECIFICATIONS, AND THE DATA CONTAINED
 * HEREIN, ARE THE EXCLUSIVE PROPERTY OF SEAGATE TECHNOLOGY
 * LIMITED, ISSUED IN STRICT CONFIDENCE AND SHALL NOT, WITHOUT
 * THE PRIOR WRITTEN PERMISSION OF SEAGATE TECHNOLOGY LIMITED,
 * BE REPRODUCED, COPIED, OR DISCLOSED TO A THIRD PARTY, OR
 * USED FOR ANY PURPOSE WHATSOEVER, OR STORED IN A RETRIEVAL SYSTEM
 * EXCEPT AS ALLOWED BY THE TERMS OF SEAGATE LICENSES AND AGREEMENTS.
 *
 * YOU SHOULD HAVE RECEIVED A COPY OF SEAGATE'S LICENSE ALONG WITH
 * THIS RELEASE. IF NOT PLEASE CONTACT A SEAGATE REPRESENTATIVE
 * http://www.seagate.com/contact
 *
 * Original author:  Arjun Hariharan <arjun.hariharan@seagate.com>
 * Original creation date: 20-May-2016
 */
package com.seagates3.dao.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;
import com.seagates3.dao.GroupDAO;
import com.seagates3.exception.DataAccessException;
import com.seagates3.model.Account;
import com.seagates3.model.Group;
import com.seagates3.util.DateUtil;

public class GroupImpl implements GroupDAO {

    private final Logger LOGGER =
            LoggerFactory.getLogger(GroupImpl.class.getName());

    /**
     * Find the group.
     *
     * @param account
     * @param groupName
     * @return
     * @throws DataAccessException
     */
    @Override
    public Group find(Account account, String groupName)
            throws DataAccessException {
        Group group = new Group();
        group.setAccount(account);
        group.setName(groupName);

        String[] attrs = {LDAPUtils.GROUP_ID, LDAPUtils.PATH,
            LDAPUtils.CREATE_TIMESTAMP};

        String ldapBase = String.format("%s=%s,%s=%s,%s=%s,%s",
                LDAPUtils.ORGANIZATIONAL_UNIT_NAME, LDAPUtils.GROUP_OU,
                LDAPUtils.ORGANIZATIONAL_NAME, account.getName(),
                LDAPUtils.ORGANIZATIONAL_UNIT_NAME, LDAPUtils.ACCOUNT_OU,
                LDAPUtils.BASE_DN
        );
        String filter = String.format("(%s=%s)", LDAPUtils.GROUP_NAME,
                groupName);

        LDAPSearchResults ldapResults;

        LOGGER.debug("Searching group dn: " + ldapBase);

        try {
            ldapResults = LDAPUtils.search(ldapBase,
                    LDAPConnection.SCOPE_SUB, filter, attrs);
        } catch (LDAPException ex) {
            LOGGER.error("Failed to find the group: " + groupName);
            throw new DataAccessException("Failed to find the group.\n" + ex);
        }

        if (ldapResults.hasMore()) {
            try {
                LDAPEntry entry = ldapResults.next();
                group.setGroupId(entry.getAttribute(LDAPUtils.GROUP_ID).
                        getStringValue());
                group.setPath(entry.getAttribute(LDAPUtils.PATH).
                        getStringValue());

                String createTimeStamp = entry.getAttribute(
                        LDAPUtils.CREATE_TIMESTAMP).getStringValue();
                String createTime = DateUtil.toServerResponseFormat(
                        createTimeStamp);
                group.setCreateDate(createTime);

            } catch (LDAPException ex) {
                LOGGER.error("Failed to find detais of group: " + groupName);
                throw new DataAccessException("Failed to find group details.\n" + ex);
            }
        }

        return group;
    }

    /**
     * Save the group.
     *
     * @param group
     * @throws DataAccessException
     */
    @Override
    public void save(Group group) throws DataAccessException {
        LDAPAttributeSet attributeSet = new LDAPAttributeSet();

        attributeSet.add(new LDAPAttribute(LDAPUtils.OBJECT_CLASS,
                LDAPUtils.GROUP_OBJECT_CLASS));
        attributeSet.add(new LDAPAttribute(LDAPUtils.GROUP_NAME,
                group.getName()));
        attributeSet.add(new LDAPAttribute(LDAPUtils.PATH, group.getPath()));
        attributeSet.add(new LDAPAttribute(LDAPUtils.GROUP_ID,
                group.getGroupId()));

        String dn = String.format("%s=%s,%s=%s,%s=%s,%s=%s,%s",
                LDAPUtils.GROUP_NAME, group.getName(),
                LDAPUtils.ORGANIZATIONAL_UNIT_NAME, LDAPUtils.GROUP_OU,
                LDAPUtils.ORGANIZATIONAL_NAME, group.getAccount().getName(),
                LDAPUtils.ORGANIZATIONAL_UNIT_NAME, LDAPUtils.ACCOUNT_OU,
                LDAPUtils.BASE_DN);

        LOGGER.debug("Saving group dn: " + dn);

        try {
            LDAPUtils.add(new LDAPEntry(dn, attributeSet));
        } catch (LDAPException ex) {
            LOGGER.error("Failed to creat the group: " + group.getName());
            throw new DataAccessException("Failed to create group.\n" + ex);
        }
    }

}
