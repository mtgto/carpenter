package net.mtgto.carpenter.domain

import org.sisioh.dddbase.core.{EntityNotFoundException, EntityResolver, Identity, Entity}
import scala.util.Try

trait BaseEntityResolver[ID <: Identity[_], T <: Entity[ID]] extends EntityResolver[ID, T] {
  /**
   * 識別子に該当するエンティティを解決する。
   *
   * @param identity 識別子
   * @return Success:
   *          エンティティ
   *         Failure:
   *          EntityNotFoundExceptionは、エンティティが見つからなかった場合
   *          RepositoryExceptionは、リポジトリにアクセスできなかった場合。
   */
  override def resolve(identity: ID): Try[T] = {
    resolveOption(identity).map(_.getOrElse(throw new EntityNotFoundException))
  }

  /**
   * 指定した識別子のエンティティが存在するかを返す。
   *
   * @param identifier 識別子
   * @return Success:
   *          存在する場合はtrue
   *         Failure:
   *          RepositoryExceptionは、リポジトリにアクセスできなかった場合。
   */
  override def contains(identifier: ID): Try[Boolean] = {
    resolveOption(identifier).map(_.isDefined)
  }
}
