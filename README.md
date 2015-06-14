orchastack-core
======
orchastack-core endeavors to make OSGI as a full featured container for distributed application, 
including **JTA**, **JPA**, **security** supports and **MQ client** for [Kafka](http://kafka.apache.org) and [rabbitMQ](http://www.rabbitmq.com/). 
It's build on [iPOJO](http://felix.apache.org/documentation/subprojects/apache-felix-ipojo.html), which is a service framework for OSGI.

Java developers just write POJO with annotaions like:
```java
@Component
@Provides(specifications = { orchastack.core.itest.biz.UserJpaService.class })
@Instantiate 
public class UserJpaServiceImpl implements UserJpaService {

   @PersistenceContext(unitName = "orcha-entity")
    private EntityManager persist;

   @RequiresRoles(value = "admins")
    @Transactional(timeout = 4000, propagation = "requires")
    public CloudUser1 saveWithTx(CloudUser1 user) throws Exception {
         ......
    }
    ......
```
 
then orchastack help you to inject the whole dependencies, and expose your OSGI service.

The JPA support is a bug-fixed version for [aries JPA](http://aries.apache.org/), codes are also simplified for efficiency.

And also a  **simple ESB **  is provided to enable standalone OSGI container as distributed. Some code 
in container A may access a OSGI service resides in container B, just like the OSGI service resides in container A.
With the help of this simple ESB, you may scale your OSGI application at will.

**Karaf fatures** are provided in orchastack.core.features project, which help you to deploy orchastack-core in [Karaf](http://karaf.apache.org).


License
-------
Licensed under the Apache License, Version 2.0
