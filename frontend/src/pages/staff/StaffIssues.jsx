import { useSearchParams } from 'react-router-dom';
import PageHeader from '../../components/PageHeader';
import IssueList from '../../components/IssueList';

const TITLES = { IN_PROGRESS: 'Issues in progress', DONE: 'Resolved issues', PENDING: 'Pending issues' };

export default function StaffIssues() {
  const [params] = useSearchParams();
  const group = params.get('group');
  return (
    <>
      <PageHeader title={TITLES[group] || 'My assigned issues'} subtitle="Only issues assigned to you are shown." />
      <IssueList endpoint="/api/staff/issues" basePath="/staff/issues" mode="staff" />
    </>
  );
}
