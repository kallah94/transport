package com.gayale.transport.tenant;

import com.gayale.transport.util.AuditableEntity;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * MongoTemplate qui ajoute automatiquement le critere {@code tenantId = <tenant courant>}
 * aux lectures, UNIQUEMENT en mode "shared". En mode "dedicated" c'est un passe-plat strict
 * (aucun critere ajoute) : le deploiement dedie est donc rigoureusement inchange.
 *
 * Ne s'applique qu'aux entites etendant AuditableEntity (donc pas Tenant ni RefreshToken).
 */
public class TenantAwareMongoTemplate extends MongoTemplate {

    private final boolean shared;

    public TenantAwareMongoTemplate(MongoDatabaseFactory factory, MongoConverter converter, boolean shared) {
        super(factory, converter);
        this.shared = shared;
    }

    private boolean scoped(@Nullable Class<?> entityClass) {
        return shared
                && entityClass != null
                && AuditableEntity.class.isAssignableFrom(entityClass)
                && StringUtils.hasText(TenantContext.getTenantId());
    }

    private Query withTenant(Query query) {
        return Query.of(query).addCriteria(Criteria.where("tenantId").is(TenantContext.getTenantId()));
    }

    @Override
    public <T> List<T> find(Query query, Class<T> entityClass, String collectionName) {
        return super.find(scoped(entityClass) ? withTenant(query) : query, entityClass, collectionName);
    }

    @Override
    public <T> List<T> findAll(Class<T> entityClass, String collectionName) {
        // findAll passe par find(Query) ici, donc le critere tenant est applique.
        return find(new Query(), entityClass, collectionName);
    }

    @Override
    public <T> T findOne(Query query, Class<T> entityClass, String collectionName) {
        return super.findOne(scoped(entityClass) ? withTenant(query) : query, entityClass, collectionName);
    }

    @Override
    public long count(Query query, @Nullable Class<?> entityClass, String collectionName) {
        return super.count(scoped(entityClass) ? withTenant(query) : query, entityClass, collectionName);
    }

    @Override
    public boolean exists(Query query, @Nullable Class<?> entityClass, String collectionName) {
        return super.exists(scoped(entityClass) ? withTenant(query) : query, entityClass, collectionName);
    }

    @Override
    public <T> T findById(Object id, Class<T> entityClass, String collectionName) {
        if (!scoped(entityClass)) {
            return super.findById(id, entityClass, collectionName);
        }
        Query q = new Query(Criteria.where("_id").is(id)
                .and("tenantId").is(TenantContext.getTenantId()));
        return super.findOne(q, entityClass, collectionName);
    }
}
