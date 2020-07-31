package tools.xor.db.common;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import tools.xor.db.base.Department;
import tools.xor.db.base.Id;

@Entity
@Table(name = "head")
public class Head extends Id
{
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

    @OneToOne
    public Department getDepartment ()
    {
        return department;
    }

    public void setDepartment (Department department)
    {
        this.department = department;
    }

    private Department department;
}
