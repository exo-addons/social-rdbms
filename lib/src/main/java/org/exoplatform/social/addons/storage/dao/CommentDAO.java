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
package org.exoplatform.social.addons.storage.dao;

import java.util.List;

import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.addons.storage.entity.Comment;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 18, 2015  
 */
public interface CommentDAO extends GenericDAO<Comment, Long> {

  /**
   * 
   * @param existingActivity
   * @param offset
   * @param limit
   * @return
   */
  List<Comment> getComments(Activity existingActivity, int offset, int limit);
  
  /**
   * 
   * @param existingActivity
   * @param sinceTime
   * @param limit
   * @return
   */
  List<Comment> getNewerOfComments(Activity existingActivity, long sinceTime, int limit);
  
  /**
   * 
   * @param existingActivity
   * @param sinceTime
   * @param limit
   * @return
   */
  List<Comment> getOlderOfComments(Activity existingActivity, long sinceTime, int limit);

  /**
   * 
   * @param existingActivity
   * @return
   */
  int getNumberOfComments(Activity existingActivity);
  
  /**
   * Get Activity parent of comment by comment's id
   * @param commentId The comment's id
   * @return
   */
  Activity findActivity(Long commentId);
}
