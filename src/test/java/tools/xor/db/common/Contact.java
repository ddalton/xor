package tools.xor.db.common;
 
import javax.persistence.Entity;
import javax.persistence.Table;

import tools.xor.db.base.Id;
 
/**
 * The persistent class for the contact database table.
 * 
 */
@Entity
@Table(name = "contact")
public class Contact extends Id {
    private String name;
    private String email;
    
    public String getName() {
    	return name;
    }
 
    public void setName(String name) {
        this.name = name;
    }
 
    public String getEmail() {
        return email;
    }
 
    public void setEmail(String email) {
        this.email = email;
    }
}