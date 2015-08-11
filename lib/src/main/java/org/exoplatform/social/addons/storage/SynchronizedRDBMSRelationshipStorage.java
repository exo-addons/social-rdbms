/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.addons.storage;

import java.util.List;

import org.exoplatform.social.addons.storage.dao.ConnectionDAO;
import org.exoplatform.social.addons.storage.dao.jpa.GenericDAOImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.storage.RelationshipStorageException;
import org.exoplatform.social.core.storage.api.IdentityStorage;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Aug 11, 2015  
 */
public class SynchronizedRDBMSRelationshipStorage extends RDBMSRelationshipStorageImpl {

  public SynchronizedRDBMSRelationshipStorage(IdentityStorage identityStorage,
                                              ConnectionDAO connectionDAO) {
    super(identityStorage, connectionDAO);
  }
  
  @Override
  public Relationship saveRelationship(final Relationship relationship) throws RelationshipStorageException {
    GenericDAOImpl.startSynchronization();
    try {
      boolean begunTx = GenericDAOImpl.startTx();
      try {
        return super.saveRelationship(relationship);
      } finally {
        GenericDAOImpl.endTx(begunTx);
      }
    } finally {
      GenericDAOImpl.stopSynchronization();
    }
  }
  
  @Override
  public void removeRelationship(Relationship relationship) throws RelationshipStorageException {
    GenericDAOImpl.startSynchronization();
    try {
      boolean begunTx = GenericDAOImpl.startTx();
      try {
        super.removeRelationship(relationship);
      } finally {
        GenericDAOImpl.endTx(begunTx);
      }
    } finally {
      GenericDAOImpl.stopSynchronization();
    }
  }
  
  @Override
  public List<Identity> getConnections(Identity identity, long offset, long limit) throws RelationshipStorageException {
    GenericDAOImpl.startSynchronization();
    try {
      return super.getConnections(identity, offset, limit);
    } finally {
      GenericDAOImpl.stopSynchronization();
    }
  }
  
  @Override
  public int getConnectionsCount(Identity identity) throws RelationshipStorageException {
    GenericDAOImpl.startSynchronization();
    try {
      return super.getConnectionsCount(identity);
    } finally {
      GenericDAOImpl.stopSynchronization();
    }
  }
  
  @Override
  public int getRelationshipsCount(Identity identity) throws RelationshipStorageException {
    GenericDAOImpl.startSynchronization();
    try {
      return super.getRelationshipsCount(identity);
    } finally {
      GenericDAOImpl.stopSynchronization();
    }
  }

}
