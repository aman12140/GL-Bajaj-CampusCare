import PageHeader from '../../components/PageHeader';
import IssueList from '../../components/IssueList';

export default function AllIssues() {
  return (
    <>
      <PageHeader title="All issues" subtitle="Review, assign and track every reported issue." />
      <IssueList endpoint="/api/issues" basePath="/admin/issues" mode="admin" />
    </>
  );
}
