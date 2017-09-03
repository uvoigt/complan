// package org.planner.eo;
//
// import java.util.Date;
//
// import javax.persistence.Access;
// import javax.persistence.AccessType;
// import javax.persistence.Column;
// import javax.persistence.Entity;
// import javax.persistence.ManyToOne;
// import javax.persistence.Temporal;
// import javax.persistence.TemporalType;
// import javax.xml.bind.annotation.XmlRootElement;
//
// import org.planner.util.NLSBundle;
// import org.planner.util.Visible;
//
// @Entity
// @Access(AccessType.FIELD)
// @XmlRootElement
// @NLSBundle("competitions")
// public class Competition extends AbstractEntity {
//
// private static final long serialVersionUID = 1L;
//
// @Column(nullable = false)
// @Visible
// private String name;
//
// @Column(nullable = false)
// @Temporal(TemporalType.DATE)
// @Visible(initial = false)
// private Date startDate;
//
// @Column(nullable = false)
// @Temporal(TemporalType.DATE)
// @Visible(initial = false)
// private Date endDate;
//
// @ManyToOne(optional = false)
// private Location location = new Location();
//
// @ManyToOne(optional = false)
// private Category category;
//
// public String getName() {
// return name;
// }
//
// public void setName(String name) {
// this.name = name;
// }
//
// public Category getCategory() {
// return category;
// }
//
// public void setCategory(Category category) {
// this.category = category;
// }
//
// public Date getStartDate() {
// return startDate;
// }
//
// public void setStartDate(Date startDate) {
// this.startDate = startDate;
// }
//
// public Date getEndDate() {
// return endDate;
// }
//
// public void setEndDate(Date endDate) {
// this.endDate = endDate;
// }
//
// public Location getLocation() {
// return location;
// }
//
// public void setLocation(Location location) {
// this.location = location;
// }
// }
