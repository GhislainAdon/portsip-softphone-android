package com.portgo.manager;

/**
 * Contact class defining an entity from the native address book or XCAP server.
 */
public class ContactExtention extends Contact{
	private String mCompany;
	private String mDepartment;
	private String mTitle;
	private String mDisname;
	/**
	 * Creates new address book
	 *
	 * @param id          a unique id defining this contact
	 * @param displayName the contact's display name
	 */
	public ContactExtention(int id, String displayName) {
		super(id, displayName);
	}

	public String getCompany(){
		return mCompany;
	}

	public String getDepartment(){
		return mDepartment;
	}
	public String getTitle(){
		return mTitle;
	}

    public void setDepartment(String mDepartment) {
        this.mDepartment = mDepartment;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public void setCompany(String mCompany) {
        this.mCompany = mCompany;
    }


//    public void addPhoneNumber(PhoneNumber number) {
//        getPhoneNumbers().add(number);
//    }

	public String getDisname() {
		return mDisname;
	}

	public void setDisname(String mDisname) {
		this.mDisname = mDisname;
	}
}
