package sz2pdf.beans;

public class SwiperPageInfoListEntry {

	Long pageId;

	Long pageNo;

	String classificationName;

	String sectionName;

	public SwiperPageInfoListEntry(Long pageId, Long pageNo, String classificationName, String sectionName) {
		super();
		this.pageId = pageId;
		this.pageNo = pageNo;
		this.classificationName = classificationName;
		this.sectionName = sectionName;
	}

	public Long getPageId() {
		return pageId;
	}

	public void setPageId(Long pageId) {
		this.pageId = pageId;
	}

	public String getClassificationName() {
		return classificationName;
	}

	public void setClassificationName(String classificationName) {
		this.classificationName = classificationName;
	}

	public String getSectionName() {
		return sectionName;
	}

	public void setSectionName(String sectionName) {
		this.sectionName = sectionName;
	}

	public Long getPageNo() {
		return pageNo;
	}

	public void setPageNo(Long pageNo) {
		this.pageNo = pageNo;
	}

	@Override
	public String toString() {
		return "SwiperPageInfoListEntry [pageId=" + pageId + ", pageNo=" + pageNo + ", classificationName="
				+ classificationName + ", sectionName=" + sectionName + "]";
	}

}
