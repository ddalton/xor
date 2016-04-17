package tools.xor.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

public interface HibernateSupport {

	CollectionPersister getCollectionPersister(SessionFactory sessionFactory,
			CollectionType collType);

	boolean isCascaded(Configuration configuration, String entityName,
			String inputPropertyName);

	Type getEntityType(SessionFactory sessionFactory, String entityName);

	Session getCurrentSession(SessionFactory sessionFactory);

}
