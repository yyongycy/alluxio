/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.master.metastore;

import alluxio.collections.Pair;
import alluxio.master.file.meta.Inode;
import alluxio.master.file.meta.InodeDirectoryView;
import alluxio.resource.CloseableIterator;

import com.esotericsoftware.minlog.Log;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.Closeable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Read-only access to the inode store.
 */
public interface KVReadOnlyInodeStore extends Closeable {
  Optional<Inode> get(long pid, String name, ReadOption option)
      throws InvalidProtocolBufferException;

  default Optional<Inode> get(long id, String name)
      throws InvalidProtocolBufferException {
    return get(id, name, ReadOption.defaults());
  }

  /**
   * Returns a closeable stream of the inodes sorted by filename of the children of the given
   *  directory that come after and including fromName.
   * @param parentId the inode id of the parent directory
   * @param fromName the inode from which to start listing
   * @return an iterator of the children starting from fromName
   */
  default CloseableIterator<? extends Inode> getChildrenFrom(
      final long parentId, final String fromName)
      throws InvalidProtocolBufferException {
    return getChildren(parentId,
        ReadOption.newBuilder().setReadFrom(fromName).build());
  }

  /**
   * Returns a closeable stream of the inodes sorted by filename of the children of the given
   *  directory that come after and including fromName.
   * @param parentId the inode id of the parent directory
   * @param prefix the prefix to match
   * @return an iterator of the children starting from fromName
   */
  default CloseableIterator<? extends Inode> getChildrenPrefix(
      final long parentId, final String prefix)
      throws InvalidProtocolBufferException {
    return getChildren(parentId,
        ReadOption.newBuilder().setPrefix(prefix).build());
  }

  /**
   * Returns a closeable stream of the inodes sorted by filename of the children of the given
   *  directory that come after and including fromName, and matching the prefix.
   * @param parentId the inode id of the parent directory
   * @param prefix the prefix to match
   * @param fromName the inode from which to start listing
   * @return an iterator of the children starting from fromName
   */
  default CloseableIterator<? extends Inode> getChildrenPrefixFrom(
      final long parentId, final String prefix, final String fromName)
      throws InvalidProtocolBufferException {
    return getChildren(parentId,
        ReadOption.newBuilder().setPrefix(prefix).setReadFrom(fromName).build());
  }

  CloseableIterator<Pair<Long, String>> getChildIds(Long inodeId, ReadOption option);

  default CloseableIterator<Pair<Long, String>> getChildIds(Long inodeId) {
    return getChildIds(inodeId, ReadOption.defaults());
  }

  /**
   * Returns an iterable for the ids of the children of the given directory.
   *
   * @param inode the inode to list child ids for
   * @param option the options
   * @return the child ids iterable
   */
  default CloseableIterator<Pair<Long, String>> getChildIds(InodeDirectoryView inode,
      ReadOption option) {
    return getChildIds(inode.getId(), option);
  }

  /**
   * Returns an iterator over the children of the specified inode.
   *
   * The iterator is weakly consistent. It can operate in the presence of concurrent modification,
   * but it is undefined whether concurrently removed inodes will be excluded or whether
   * concurrently added inodes will be included.
   *
   * @param inodeId an inode id
   * @param option the options
   * @return an iterable over the children of the inode with the given id
   */
  default CloseableIterator<? extends Inode> getChildren(Long inodeId, ReadOption option) {
    CloseableIterator<Pair<Long, String>> it = getChildIds(inodeId, option);
    Iterator<Inode> iter =  new Iterator<Inode>() {
      private Inode mNext = null;
      @Override
      public boolean hasNext() {
        try {
          advance();
        } catch (InvalidProtocolBufferException e) {
          Log.debug("Found exception {}",e);
        }
        return mNext != null;
      }

      @Override
      public Inode next() {
        if (!hasNext()) {
          throw new NoSuchElementException(
              "No more children in iterator for inode id " + inodeId);
        }
        Inode next = mNext;
        mNext = null;
        return next;
      }

      void advance() throws InvalidProtocolBufferException {
        while (mNext == null && it.hasNext()) {
          Pair<Long, String> nextId = it.next();
          // Make sure the inode metadata still exists
          Optional<Inode> nextInode = get(nextId.getFirst(), nextId.getSecond(), option);
          nextInode.ifPresent(inode -> mNext = inode);
        }
      }
    };
    return CloseableIterator.create(iter, (any) -> it.close());
  }

  /**
   * @param inodeId an inode id
   * @return the result of {@link #getChildren(Long, ReadOption)} with default option
   */
  default CloseableIterator<? extends Inode> getChildren(Long inodeId) {
    return getChildren(inodeId, ReadOption.defaults());
  }

  /**
   * @param inode an inode directory
   * @param option the options
   * @return an iterable over the children of the inode with the given id
   */
  default CloseableIterator<? extends Inode> getChildren(
      InodeDirectoryView inode, ReadOption option) {
    return getChildren(inode.getId(), option);
  }

  /**
   * @param inode an inode directory
   * @return the result of {@link #getChildren(InodeDirectoryView, ReadOption)} with default option
   */
  default CloseableIterator<? extends Inode> getChildren(InodeDirectoryView inode)
      throws InvalidProtocolBufferException {
    return getChildren(inode.getId(), ReadOption.defaults());
  }

  /**
   * @param inodeId an inode id
   * @param name an inode name
   * @param option the options
   * @return the id of the child of the inode with the given name
   */
  Optional<Long> getChildId(Long inodeId, String name, ReadOption option);

  /**
   * @param inodeId an inode id
   * @param name an inode name
   * @return the result of {@link #getChildId(Long, String, ReadOption)} with default option
   */
  default Optional<Long> getChildId(Long inodeId, String name) {
    return getChildId(inodeId, name, ReadOption.defaults());
  }

  /**
   * @param inode an inode directory
   * @param name an inode name
   * @param option the options
   * @return the id of the child of the inode with the given name
   */
  default Optional<Long> getChildId(InodeDirectoryView inode, String name, ReadOption option)
      throws InvalidProtocolBufferException {
    return getChildId(inode.getId(), name, option);
  }

  /**
   * @param inodeId an inode id
   * @param name an inode name
   * @param option the options
   * @return the child of the inode with the given name
   */
  Optional<Inode> getChild(Long inodeId, String name, ReadOption option);

  /**
   * @param inodeId an inode id
   * @param name an inode name
   * @return the result of {@link #getChild(Long, String, ReadOption)} with default option
   */
  default Optional<Inode> getChild(Long inodeId, String name) {
    return getChild(inodeId, name, ReadOption.defaults());
  }

  /**
   * @param inode an inode directory
   * @param name an inode name
   * @param option the options
   * @return the child of the inode with the given name
   */
  default Optional<Inode> getChild(InodeDirectoryView inode, String name, ReadOption option) {
    return getChild(inode.getId(), name, option);
  }

  /**
   * @param inode an inode directory
   * @param name an inode name
   * @return the result of {@link #getChild(InodeDirectoryView, String, ReadOption)} with default
   *    option
   */
  default Optional<Inode> getChild(InodeDirectoryView inode, String name) {
    return getChild(inode.getId(), name, ReadOption.defaults());
  }

  /**
   * @param inode an inode directory
   * @param option the options
   * @return whether the inode has any children
   */
  boolean hasChildren(InodeDirectoryView inode, ReadOption option);

  /**
   * @param inode an inode directory
   * @return the result of {@link #hasChildren(InodeDirectoryView, ReadOption)} with default option
   */
  default boolean hasChildren(InodeDirectoryView inode) {
    return hasChildren(inode, ReadOption.defaults());
  }
}
