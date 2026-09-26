import { Link } from 'react-router-dom';
import PageHeader from '../../components/PageHeader';
import IssueList from '../../components/IssueList';

export default function MyIssues() {
  return (
    <>
      <PageHeader title="My issues" subtitle="Every issue you have reported, with its current status."
                  actions={<Link className="btn btn-primary" to="/student/report">Report an issue</Link>} />
      <IssueList endpoint="/api/student/issues" basePath="/student/issues" mode="student" />
    </>
  );
}
